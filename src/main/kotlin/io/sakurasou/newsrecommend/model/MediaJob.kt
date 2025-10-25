package io.sakurasou.newsrecommend.model

import java.time.LocalDateTime

data class MediaJob(
    val id: Long? = null,
    val userId: Long,
    val type: MediaJobType,
    val filePath: String,
    var status: MediaJobStatus,
    var resultText: String? = null,
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null,
)

enum class MediaJobType {
    STT,
    OCR,
}

enum class MediaJobStatus {
    PENDING,
    RUNNING,
    DONE,
    FAILED,
}
