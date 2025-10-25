package io.sakurasou.newsrecommend.dto

data class StructuredArticleDTO(
    val title: String,
    val author: String? = null,
    val content: String,
    val publishTime: String? = null,
)
