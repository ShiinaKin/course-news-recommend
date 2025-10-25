package io.sakurasou.newsrecommend.model

import java.time.LocalDateTime

data class User(
    val id: Long? = null,
    val username: String,
    val passwordHash: String,
    val nickname: String,
    val createdAt: LocalDateTime? = null,
)
