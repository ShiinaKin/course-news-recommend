package io.sakurasou.newsrecommend.controller

import io.sakurasou.newsrecommend.dto.RSSImportRequest
import io.sakurasou.newsrecommend.dto.RSSImportResponse
import io.sakurasou.newsrecommend.service.RSSFeedResult
import io.sakurasou.newsrecommend.service.RSSFeedClient
import io.sakurasou.newsrecommend.service.RssFetchException
import io.sakurasou.newsrecommend.service.RSSImportService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/rss")
class RSSController(
    private val rssFetchService: RSSFeedClient,
    private val rssImportService: RSSImportService,
) {

    @PostMapping("/fetch")
    fun fetchRSS(@RequestBody request: RssFetchRequest): ResponseEntity<RSSFeedResult> {
        val url = request.rssLink?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "rssLink 不能为空")
        val result = try {
            rssFetchService.fetch(url)
        } catch (ex: RssFetchException) {
            throw ResponseStatusException(ex.status, ex.message, ex)
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.message, ex)
        }
        return ResponseEntity.ok(result)
    }

    @PostMapping("/import")
    fun importRSS(@RequestBody request: RSSImportRequest): ResponseEntity<RSSImportResponse> {
        val url = request.rssLink.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "rssLink 不能为空")
        val count = try {
            rssImportService.import(url)
        } catch (ex: RssFetchException) {
            throw ResponseStatusException(ex.status, ex.message, ex)
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.message, ex)
        }
        return ResponseEntity.ok(RSSImportResponse(importedCount = count))
    }
}

data class RssFetchRequest(
    val rssLink: String?,
)
