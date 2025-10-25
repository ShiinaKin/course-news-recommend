package io.sakurasou.newsrecommend.dto

data class TitleClassificationRequest(
    val title: String,
    val topK: Int? = null,
)

data class TagPredictionDTO(
    val tagId: Long,
    val tagName: String,
    val score: Double,
)

data class TitleClassificationResponse(
    val title: String,
    val predictions: List<TagPredictionDTO>,
)
