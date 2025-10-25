package io.sakurasou.newsrecommend.model

import java.time.LocalDateTime

data class UserEvent(
    val id: Long? = null,
    val userId: Long,
    val articleId: Long,
    val eventType: String,
    val createdAt: LocalDateTime? = null,
)
