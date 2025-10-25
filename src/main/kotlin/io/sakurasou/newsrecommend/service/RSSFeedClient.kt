package io.sakurasou.newsrecommend.service

interface RSSFeedClient {
    fun fetch(url: String): RSSFeedResult
}
