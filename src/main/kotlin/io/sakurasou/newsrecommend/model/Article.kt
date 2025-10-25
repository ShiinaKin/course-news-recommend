package io.sakurasou.newsrecommend.model

import java.time.LocalDateTime

data class Article(
    val id: Long? = null,
    val title: String,
    val content: String,
    val source: String? = null,
    val publishTime: LocalDateTime? = null,
    val createdAt: LocalDateTime? = null,
)
