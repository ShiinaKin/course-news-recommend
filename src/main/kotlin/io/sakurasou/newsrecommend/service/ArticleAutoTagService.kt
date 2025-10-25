package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.dao.ArticleTagDAO
import io.sakurasou.newsrecommend.model.ArticleTag
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArticleAutoTagService(
    private val articleTagDao: ArticleTagDAO,
    private val titleClassifierService: TitleClassifierService,
    private val taggingService: TaggingService,
) {

    private val logger = LoggerFactory.getLogger(ArticleAutoTagService::class.java)

    @Transactional
    fun applyByTitle(articleId: Long, title: String, fallbackContent: String? = null, topK: Int = DEFAULT_TOP_K) {
        articleTagDao.deleteByArticleId(articleId)
        val predictions = try {
            titleClassifierService.predictTags(title, topK)
        } catch (ex: Exception) {
            logger.warn("Failed to predict tags by title: {}", ex.message)
            emptyList()
        }
        if (predictions.isNotEmpty()) {
            val maxScore = predictions.maxOf { it.score }.takeIf { it > 0 } ?: 1.0
            predictions.forEach { prediction ->
                val normalized = (prediction.score / maxScore).coerceIn(0.1, 1.0)
                articleTagDao.insert(
                    ArticleTag(
                        articleId = articleId,
                        tagId = prediction.tagId,
                        weight = normalized,
                    ),
                )
            }
        } else if (!fallbackContent.isNullOrBlank()) {
            taggingService.applyTags(articleId, fallbackContent)
        }
    }

    private companion object {
        private const val DEFAULT_TOP_K = 3
    }
}
