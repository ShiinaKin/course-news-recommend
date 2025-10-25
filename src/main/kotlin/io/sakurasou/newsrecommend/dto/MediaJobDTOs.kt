package io.sakurasou.newsrecommend.dto

import io.sakurasou.newsrecommend.model.MediaJobStatus
import io.sakurasou.newsrecommend.model.MediaJobType
import java.time.LocalDateTime

data class MediaJobResponse(
    val id: Long,
    val type: MediaJobType,
    val status: MediaJobStatus,
    val resultText: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
