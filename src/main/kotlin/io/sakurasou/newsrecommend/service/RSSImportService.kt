package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.dao.ArticleDAO
import io.sakurasou.newsrecommend.model.Article
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Service
class RSSImportService(
    private val rssFeedClient: RSSFeedClient,
    private val articleDao: ArticleDAO,
    private val articleAutoTagService: ArticleAutoTagService,
    private val titleClassifierService: TitleClassifierService,
) {

    private val logger = LoggerFactory.getLogger(RSSImportService::class.java)

    @Transactional
    fun import(url: String, topK: Int = DEFAULT_TOP_K): Int {
        val feed = rssFeedClient.fetch(url)
        if (feed.items.isEmpty()) {
            logger.info("RSS import {} skipped: empty feed", url)
            return 0
        }
        var success = 0
        feed.items.forEach { item ->
            runCatching {
                val title = item.title?.trim().orEmpty()
                if (title.isBlank()) {
                    return@forEach
                }
                if (articleDao.findByTitle(title) != null) {
                    logger.debug("RSS import skip duplicate title: {}", title)
                    return@forEach
                }
                val content = normalizeContent(item.description) ?: title
                val publishTime = parsePubDate(item.pubDate)
                val article = Article(
                    title = title,
                    content = content,
                    source = item.link,
                    publishTime = publishTime,
                    createdAt = LocalDateTime.now(),
                )
                articleDao.insert(article)
                val persisted = articleDao.findByTitle(title)
                    ?: run {
                        logger.warn("RSS import inserted but not found by title: {}", title)
                        return@forEach
                    }
                val articleId = persisted.id ?: return@forEach
                articleAutoTagService.applyByTitle(articleId, title, content, topK)
                success++
            }.onFailure {
                it.printStackTrace()
            }
        }
        if (success > 0) {
            titleClassifierService.reloadModel()
        }
        return success
    }

    private fun parsePubDate(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank()) {
            return null
        }
        val candidates = listOf(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME,
        )
        val trimmed = raw.trim()
        candidates.forEach { formatter ->
            try {
                return ZonedDateTime.parse(trimmed, formatter).toLocalDateTime()
            } catch (_: DateTimeParseException) {
            }
        }
        return try {
            LocalDateTime.parse(trimmed)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun normalizeContent(description: String?): String? {
        if (description.isNullOrBlank()) {
            return null
        }
        val noHtml = HTML_TAG_REGEX.replace(description, "").trim()
        return noHtml.ifBlank { null }
    }

    private companion object Companion {
        private val HTML_TAG_REGEX = "<[^>]+>".toRegex()
        private const val DEFAULT_TOP_K = 3
    }
}
