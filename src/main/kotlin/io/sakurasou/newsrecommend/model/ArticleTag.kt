package io.sakurasou.newsrecommend.model

data class ArticleTag(
    val articleId: Long,
    val tagId: Long,
    val weight: Double,
)
