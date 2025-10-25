package io.sakurasou.newsrecommend.dto

data class RSSImportRequest(
    val rssLink: String,
)

data class RSSImportResponse(
    val importedCount: Int,
)
