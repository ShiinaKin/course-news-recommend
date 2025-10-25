package io.sakurasou.newsrecommend.controller

import io.sakurasou.newsrecommend.dto.MediaJobResponse
import io.sakurasou.newsrecommend.model.MediaJob
import io.sakurasou.newsrecommend.model.MediaJobType
import io.sakurasou.newsrecommend.service.MediaJobService
import io.sakurasou.newsrecommend.service.UserService
import io.sakurasou.newsrecommend.util.FileStorage
import io.sakurasou.newsrecommend.util.SecurityUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Path

@RestController
@RequestMapping("/api")
class UploadController(
    private val mediaJobService: MediaJobService,
    private val userService: UserService,
) {

    private val uploadDir = Path.of("uploads")
    private val allowedAudioExtensions = setOf(".mp3", ".wav")
    private val allowedAudioContentTypes = setOf(
        "audio/mpeg",
        "audio/mp3",
        "audio/wav",
        "audio/x-wav",
        "audio/wave",
        "audio/x-pn-wav",
    )

    @PostMapping("/upload/audio", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadAudio(file: MultipartFile): MediaJobResponse {
        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "文件不能为空")
        }
        if (!isAllowedAudio(file)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持上传 mp3 或 wav 音频文件")
        }
        val userId = requireUserId()
        val stored = FileStorage.saveUpload(uploadDir, file)
        val job = mediaJobService.submitJob(userId, MediaJobType.STT, stored)
        return job.toResponse()
    }

    @PostMapping("/upload/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(file: MultipartFile): MediaJobResponse {
        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "文件不能为空")
        }
        val userId = requireUserId()
        val stored = FileStorage.saveUpload(uploadDir, file)
        val job = mediaJobService.submitJob(userId, MediaJobType.OCR, stored)
        return job.toResponse()
    }

    @GetMapping("/jobs/{id}")
    fun getJob(@PathVariable id: Long): MediaJobResponse {
        val job = mediaJobService.findJob(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在")
        if (requireUserId() != job.userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "无法访问他人任务")
        }
        return job.toResponse()
    }

    private fun requireUserId(): Long {
        val username = SecurityUtils.currentUsername()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录")
        val user = userService.findByUsername(username)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录")
        return user.id ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录")
    }

    private fun MediaJob.toResponse() = MediaJobResponse(
        id = this.id!!,
        type = this.type,
        status = this.status,
        resultText = this.resultText,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )

    private fun isAllowedAudio(file: MultipartFile): Boolean {
        val originalName = file.originalFilename?.lowercase() ?: ""
        val contentType = file.contentType?.lowercase()
        val extensionCheck = allowedAudioExtensions.any { originalName.endsWith(it) }
        val contentTypeCheck = contentType != null && contentType in allowedAudioContentTypes
        return extensionCheck || contentTypeCheck
    }
}
