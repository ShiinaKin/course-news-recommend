package io.sakurasou.newsrecommend.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.sakurasou.newsrecommend.dto.StructuredArticleDTO
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists

@Service
class StructureTextService(
    private val objectMapper: ObjectMapper
) {

    private val scriptPath = Path.of("tools", "structure_text", "main.py").toAbsolutePath().normalize()

    fun structureFromText(rawText: String): StructuredArticleDTO {
        val trimmed = rawText.trim()
        require(trimmed.isNotEmpty()) { "Input text must not be blank" }
        val tempFile = Files.createTempFile("structured-article-", ".txt")
        return try {
            Files.writeString(tempFile, trimmed, StandardCharsets.UTF_8)
            structureFromFile(tempFile)
        } finally {
            try {
                Files.deleteIfExists(tempFile)
            } catch (_: Exception) {
                // best effort cleanup; ignore failures
            }
        }
    }

    fun structureFromFile(inputFile: Path): StructuredArticleDTO {
        require(Files.exists(inputFile)) { "Input file not found: $inputFile" }
        val apiKey = System.getenv("OPENAI_API_KEY")
            ?: throw IllegalStateException("OPENAI_API_KEY environment variable is not configured")
        val pythonExecutable = resolvePythonExecutable()
        require(pythonExecutable.exists()) { "Python executable not found: $pythonExecutable" }
        require(Files.exists(scriptPath)) { "Structure text script not found: $scriptPath" }

        val process = ProcessBuilder(
            pythonExecutable.toAbsolutePath().normalize().toString(),
            scriptPath.toString(),
            apiKey,
            inputFile.toAbsolutePath().normalize().toString(),
        )
            .directory(Path.of("").toAbsolutePath().toFile())
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }.trim()
        val finished = process.waitFor(Duration.ofMinutes(5).toMillis(), TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroyForcibly()
            throw IllegalStateException("structure_text CLI timed out")
        }
        if (process.exitValue() != 0) {
            throw IllegalStateException(
                "structure_text CLI failed with exit ${process.exitValue()}: $output",
            )
        }
        if (output.isBlank()) {
            throw IllegalStateException("structure_text CLI produced empty output")
        }

        return objectMapper.readValue<StructuredArticleDTO>(output)
    }

    private fun resolvePythonExecutable(): Path {
        val unixPath = Path.of("tools", "structure_text", ".venv", "bin", "python")
        if (Files.exists(unixPath)) {
            return unixPath
        }
        val windowsPath = Path.of("tools", "structure_text", ".venv", "Scripts", "python.exe")
        if (Files.exists(windowsPath)) {
            return windowsPath
        }
        return unixPath
    }
}
