package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.config.AppProperties
import io.sakurasou.newsrecommend.util.JsonUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

interface OCRService {
    fun recognize(filePath: Path): String
}

@Service
class VisionOCRService(
    private val properties: AppProperties,
) : OCRService {

    private val logger = LoggerFactory.getLogger(VisionOCRService::class.java)

    override fun recognize(filePath: Path): String {
        require(Files.exists(filePath)) { "文件不存在: $filePath" }
        val command = mutableListOf(
            properties.ocr.binPath,
            filePath.toAbsolutePath().toString(),
        )
        if (properties.ocr.langs.isNotBlank()) {
            command.add("--langs")
            command.add(properties.ocr.langs)
        }
        if (properties.ocr.fast) {
            command.add("--fast")
        } else {
            command.add("--accurate")
        }
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                output.appendLine(line)
                line = reader.readLine()
            }
        }
        val finished = process.waitFor(Duration.ofSeconds(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroyForcibly()
            throw IllegalStateException("OCR 处理超时")
        }
        if (process.exitValue() != 0) {
            logger.error("OCR 失败: {}", output.toString())
            throw IllegalStateException("OCR 执行失败: ${process.exitValue()}")
        }
        val json = output.toString()
        val tree = JsonUtils.parseTree(json)
        val lines = tree["lines"] ?: return ""
        val builder = StringBuilder()
        lines.forEach { node ->
            val text = node["text"]?.asText()
            if (!text.isNullOrBlank()) {
                builder.appendLine(text.trim())
            }
        }
        return builder.toString().trim()
    }
}
