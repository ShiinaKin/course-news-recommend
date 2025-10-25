package io.sakurasou.newsrecommend.controller

import io.sakurasou.newsrecommend.dto.ArticleDetailDTO
import io.sakurasou.newsrecommend.dto.ArticleSummaryDTO
import io.sakurasou.newsrecommend.dto.PagedArticleResponse
import io.sakurasou.newsrecommend.dto.TagPredictionDTO
import io.sakurasou.newsrecommend.dto.TitleClassificationRequest
import io.sakurasou.newsrecommend.dto.TitleClassificationResponse
import io.sakurasou.newsrecommend.service.ArticleService
import io.sakurasou.newsrecommend.service.RecommendService
import io.sakurasou.newsrecommend.service.TitleClassifierService
import io.sakurasou.newsrecommend.service.UserService
import io.sakurasou.newsrecommend.service.UserTagService
import io.sakurasou.newsrecommend.util.SecurityUtils
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class ArticleController(
    private val articleService: ArticleService,
    private val recommendService: RecommendService,
    private val titleClassifierService: TitleClassifierService,
    private val userService: UserService,
    private val userTagService: UserTagService,
) {

    @GetMapping("/api/articles")
    fun listArticles(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "time") orderBy: String,
    ): PagedArticleResponse {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 50)
        if (orderBy == "reco") {
            val username = SecurityUtils.currentUsername()
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录用户无法获取个性化推荐")
            val user = userService.findByUsername(username)
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录用户无法获取个性化推荐")
            val items = recommendService.recommendForUser(user.id!!, safeSize)
            return PagedArticleResponse(page = 0, size = items.size, total = items.size.toLong(), items = items)
        }
        val (articles, total) = articleService.listArticles(orderBy, safePage, safeSize)
        val tagMap = articleService.getArticlesTags(articles.mapNotNull { it.id })
        val tagNames = userTagService.listAllTags().associateBy({ it.id }, { it.name })
        val summaries = articles.map { article ->
            ArticleSummaryDTO(
                id = article.id!!,
                title = article.title,
                summary = article.content.take(200),
                publishTime = article.publishTime,
                tags = (tagMap[article.id] ?: emptyList()).mapNotNull { tagNames[it.tagId] },
                score = null,
            )
        }
        return PagedArticleResponse(page = safePage, size = safeSize, total = total, items = summaries)
    }

    @PostMapping("/api/articles/infer-tags")
    fun inferTags(@RequestBody request: TitleClassificationRequest): TitleClassificationResponse {
        if (request.title.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "标题不能为空")
        }
        val topK = request.topK?.coerceIn(1, 10) ?: 3
        val predictions = titleClassifierService.predictTags(request.title, topK)
        return TitleClassificationResponse(
            title = request.title,
            predictions = predictions.map {
                TagPredictionDTO(tagId = it.tagId, tagName = it.tagName, score = it.score)
            },
        )
    }

    @GetMapping("/api/articles/{id}")
    fun getArticle(@PathVariable id: Long): ArticleDetailDTO {
        val article = articleService.getArticleById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在")
        val username = SecurityUtils.currentUsername()
        val related = if (username != null) {
            val user = userService.findByUsername(username)
            if (user?.id != null) {
                articleService.recordEvent(user.id, id, "VIEW")
                recommendService.recommendForUser(user.id, 5)
            } else {
                defaultRelated()
            }
        } else {
            defaultRelated()
        }
        return articleService.buildDetailDTO(article, related)
    }

    private fun defaultRelated(): List<ArticleSummaryDTO> {
        val recent = articleService.listRecent(5)
        val tags = articleService.getArticlesTags(recent.mapNotNull { it.id })
        val tagNames = userTagService.listAllTags().associateBy({ it.id }, { it.name })
        return recent.map { article ->
            ArticleSummaryDTO(
                id = article.id!!,
                title = article.title,
                summary = article.content.take(200),
                publishTime = article.publishTime,
                tags = (tags[article.id] ?: emptyList()).mapNotNull { tagNames[it.tagId] },
                score = null,
            )
        }
    }
}
