package io.sakurasou.newsrecommend.util

import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

object FileStorage {
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")

    fun saveUpload(baseDir: Path, file: MultipartFile): Path {
        val timestamp = LocalDateTime.now().format(formatter)
        val sanitized = file.originalFilename?.replace(Regex("[^A-Za-z0-9._-]"), "_") ?: "upload.bin"
        val targetDir = baseDir.resolve(timestamp.take(8))
        if (!targetDir.exists()) {
            targetDir.createDirectories()
        }
        val target = targetDir.resolve("${timestamp}_$sanitized")
        file.inputStream.use { input ->
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        }
        return target
    }
}
