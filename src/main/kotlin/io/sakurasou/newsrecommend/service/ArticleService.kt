package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.dao.ArticleDAO
import io.sakurasou.newsrecommend.dao.ArticleTagDAO
import io.sakurasou.newsrecommend.dao.TagDAO
import io.sakurasou.newsrecommend.dao.UserEventDAO
import io.sakurasou.newsrecommend.dto.ArticleDetailDTO
import io.sakurasou.newsrecommend.dto.ArticleSummaryDTO
import io.sakurasou.newsrecommend.model.Article
import io.sakurasou.newsrecommend.model.ArticleTag
import io.sakurasou.newsrecommend.model.UserEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ArticleService(
    private val articleDao: ArticleDAO,
    private val articleTagDao: ArticleTagDAO,
    private val tagDao: TagDAO,
    private val userEventDao: UserEventDAO,
    private val articleAutoTagService: ArticleAutoTagService,
    private val userTagService: UserTagService,
) {

    fun listArticles(orderBy: String, page: Int, size: Int): Pair<List<Article>, Long> {
        val offset = page * size
        val items = when (orderBy) {
            "time" -> articleDao.listByPublishTime(offset, size)
            else -> articleDao.listByCreatedAt(offset, size)
        }
        val total = articleDao.countAll()
        return items to total
    }

    fun getArticleById(id: Long): Article? = articleDao.findById(id)

    fun getArticleTags(articleId: Long): List<ArticleTag> = articleTagDao.findByArticleId(articleId)

    fun getArticlesTags(articleIds: List<Long>): Map<Long, List<ArticleTag>> {
        if (articleIds.isEmpty()) {
            return emptyMap()
        }
        val all = articleTagDao.findByArticleIds(articleIds)
        return all.groupBy { it.articleId }
    }

    fun listRecent(limit: Int): List<Article> = articleDao.findRecent(limit)

    @Transactional
    fun recordEvent(userId: Long, articleId: Long, eventType: String) {
        userEventDao.insert(
            UserEvent(
                userId = userId,
                articleId = articleId,
                eventType = eventType,
                createdAt = LocalDateTime.now(),
            ),
        )
        if (eventType == "VIEW" || eventType == "CLICK") {
            val tags = articleTagDao.findByArticleId(articleId)
            if (tags.isNotEmpty()) {
                val increments = tags.associate { it.tagId to 0.1 * it.weight.coerceAtMost(1.0) }
                userTagService.incrementWeights(userId, increments)
            }
        }
    }

    @Transactional
    fun createGeneratedArticle(
        userId: Long,
        type: String,
        title: String,
        content: String,
        source: String,
        publishTime: LocalDateTime? = null,
    ): Article {
        val article = Article(
            title = title,
            content = content,
            source = source,
            publishTime = publishTime ?: LocalDateTime.now(),
        )
        articleDao.insert(article)
        val articleId = article.id ?: throw IllegalStateException("文章保存失败")
        articleAutoTagService.applyByTitle(articleId, title, content)
        recordEvent(userId, articleId, "UPLOAD")
        return articleDao.findById(articleId) ?: article.copy(id = articleId)
    }

    fun buildDetailDTO(article: Article, related: List<ArticleSummaryDTO>): ArticleDetailDTO {
        val tagIds = getArticleTags(article.id!!).map { it.tagId }
        val tagNames = if (tagIds.isEmpty()) emptyList() else tagDao.findByIds(tagIds).map { it.name }
        val summary = related.map {
            it.copy(summary = it.summary.take(200))
        }
        return ArticleDetailDTO(
            id = article.id,
            title = article.title,
            content = article.content,
            source = article.source,
            publishTime = article.publishTime,
            tags = tagNames,
            related = summary,
        )
    }
}
