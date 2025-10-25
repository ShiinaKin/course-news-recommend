package io.sakurasou.newsrecommend.model

data class UserTagView(
    val tagId: Long,
    val tagName: String,
    val weight: Double,
)
