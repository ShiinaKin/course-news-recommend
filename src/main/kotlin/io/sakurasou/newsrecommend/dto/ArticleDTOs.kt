package io.sakurasou.newsrecommend.dto

import java.time.LocalDateTime

data class ArticleSummaryDTO(
    val id: Long,
    val title: String,
    val summary: String,
    val publishTime: LocalDateTime?,
    val tags: List<String>,
    val score: Double? = null,
)

data class ArticleDetailDTO(
    val id: Long,
    val title: String,
    val content: String,
    val source: String?,
    val publishTime: LocalDateTime?,
    val tags: List<String>,
    val related: List<ArticleSummaryDTO>,
)

data class PagedArticleResponse(
    val page: Int,
    val size: Int,
    val total: Long,
    val items: List<ArticleSummaryDTO>,
)
