package io.sakurasou.newsrecommend.model

import java.time.LocalDateTime

data class UserTag(
    val userId: Long,
    val tagId: Long,
    val weight: Double,
    val updatedAt: LocalDateTime,
)
