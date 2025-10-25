package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit

interface STTService {
    fun transcribe(jobId: Long, filePath: Path): String
}

@Service
class ConfigurableSTTService(
    private val properties: AppProperties,
) : STTService {

    private val logger = LoggerFactory.getLogger(ConfigurableSTTService::class.java)

    override fun transcribe(jobId: Long, filePath: Path): String {
        return when (properties.stt.mode.lowercase()) {
            "real" -> realTranscribe(jobId, filePath)
            else -> mockTranscribe(filePath)
        }
    }

    private fun mockTranscribe(filePath: Path): String {
        logger.info("Mock STT processing for {}", filePath.fileName)
        return "【模拟转写】文件 ${filePath.fileName} 于 ${System.currentTimeMillis()} 完成转写。"
    }

    private fun realTranscribe(jobId: Long, filePath: Path): String {
        logger.info("Real STT requested for job {} path {}", jobId, filePath)
        if (properties.stt.modelPath.isBlank()) {
            throw IllegalStateException("未配置 Whisper 模型路径")
        }
        val binaryPath = resolveBinary()
        require(Files.exists(filePath)) { "音频文件不存在: $filePath" }

        val tempDir = Files.createTempDirectory("whisper-")
        val rawName = filePath.fileName.toString().substringBeforeLast('.', filePath.fileName.toString())
        val sanitizedName = rawName.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "media" }
        val outputBase = tempDir.resolve("${sanitizedName}_text")
        val outputTxt = Path.of("$outputBase.txt")

        val command = mutableListOf(
            binaryPath.toString(),
            "-m", properties.stt.modelPath,
            "-f", filePath.toAbsolutePath().toString(),
        )
        if (properties.stt.language.isNotBlank()) {
            command.addAll(listOf("-l", properties.stt.language))
        }
        command.addAll(
            listOf(
                "-otxt",
                "-of", outputBase.toAbsolutePath().toString(),
                "-np",
                "-nt",
            ),
        )
        logger.debug("Invoking whisper-cli: {}", command.joinToString(" "))
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val outputText = buildString {
            BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    appendLine(line)
                    line = reader.readLine()
                }
            }
        }
        val completed = process.waitFor(Duration.ofMinutes(5).toMillis(), TimeUnit.MILLISECONDS)
        if (!completed) {
            process.destroyForcibly()
            cleanupFiles(outputTxt, tempDir)
            throw IllegalStateException("whisper-cli 处理超时")
        }
        if (process.exitValue() != 0) {
            cleanupFiles(outputTxt, tempDir)
            logger.error("whisper-cli 失败: {}", outputText)
            throw IllegalStateException("whisper-cli 执行失败: exit=${process.exitValue()}")
        }

        val transcript = try {
            if (!Files.exists(outputTxt)) {
                logger.error("未找到 whisper-cli 输出文件: {}", outputTxt)
                throw IllegalStateException("未获取到转写文本")
            }
            val text = Files.readString(outputTxt, StandardCharsets.UTF_8).trim()
            if (text.isBlank()) {
                logger.warn("whisper-cli 输出文件为空: {}，原始输出: {}", outputTxt, outputText)
                throw IllegalStateException("未获取到转写文本")
            }
            text
        } finally {
            cleanupFiles(outputTxt, tempDir)
        }
        return transcript
    }

    private fun cleanupFiles(outputTxt: Path, tempDir: Path) {
        try {
            Files.deleteIfExists(outputTxt)
        } catch (ex: Exception) {
            logger.warn("删除临时转写文件失败: {}", outputTxt, ex)
        }
        try {
            Files.deleteIfExists(tempDir)
        } catch (ex: Exception) {
            logger.debug("删除临时目录失败: {}", tempDir, ex)
        }
    }

    private fun resolveBinary(): Path {
        val configured = properties.stt.binaryPath.takeIf { it.isNotBlank() }
        val envBinary = System.getenv("whisper")?.takeIf { it.isNotBlank() }
        val envRoot = System.getenv("WHISPER_CPP")?.takeIf { it.isNotBlank() }?.let {
            Path.of(it, "build", "bin", "whisper-cli").toString()
        }
        val candidate = configured ?: envBinary ?: envRoot
        require(!candidate.isNullOrBlank()) {
            "未找到 whisper-cli 路径，请配置 app.stt.binaryPath 或设置环境变量 whisper/WHISPER_CPP"
        }
        val path = Path.of(candidate)
        val absolute = path.toAbsolutePath().normalize()
        if (!Files.exists(absolute)) {
            throw IllegalStateException("whisper-cli 未找到: $absolute")
        }
        if (!Files.isExecutable(absolute)) {
            throw IllegalStateException("whisper-cli 不可执行: $absolute")
        }
        return absolute
    }
}
