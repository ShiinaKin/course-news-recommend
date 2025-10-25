package io.sakurasou.newsrecommend.dto

data class RecommendRequest(
    val topK: Int = 10,
)

data class RecommendResponse(
    val items: List<ArticleSummaryDTO>,
)
