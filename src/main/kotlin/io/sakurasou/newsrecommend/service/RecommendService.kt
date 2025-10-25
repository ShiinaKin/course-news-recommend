package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.config.AppProperties
import io.sakurasou.newsrecommend.dao.ArticleDAO
import io.sakurasou.newsrecommend.dao.ArticleTagDAO
import io.sakurasou.newsrecommend.dao.TagDAO
import io.sakurasou.newsrecommend.dao.UserEventDAO
import io.sakurasou.newsrecommend.dao.UserTagDAO
import io.sakurasou.newsrecommend.dto.ArticleSummaryDTO
import io.sakurasou.newsrecommend.model.Article
import io.sakurasou.newsrecommend.util.CosineSimilarity
import io.sakurasou.newsrecommend.util.TFIDFVectorizer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.exp
import kotlin.math.max

@Service
class RecommendService(
    private val appProperties: AppProperties,
    private val articleDao: ArticleDAO,
    private val articleTagDao: ArticleTagDAO,
    private val userEventDao: UserEventDAO,
    private val userTagDao: UserTagDAO,
    private val tagDao: TagDAO,
) {

    private val logger = LoggerFactory.getLogger(RecommendService::class.java)
    private val vectorizer = TFIDFVectorizer()

    fun recommendForUser(userId: Long, topK: Int): List<ArticleSummaryDTO> {
        val candidates = articleDao.findRecent(appProperties.reco.candidateLimit)
        if (candidates.isEmpty()) {
            return emptyList()
        }
        val vectors = buildTfidfVectors(candidates)
        val profileVector = buildUserProfileVector(userId, vectors)
        val userTagVector = buildUserTagVector(userId)
        val articleTagVectors = buildArticleTagVectors(candidates)
        val now = LocalDateTime.now()
        val tauHours = max(1.0, appProperties.reco.recentDays * 24.0)
        val alpha = appProperties.reco.alpha
        val beta = appProperties.reco.beta
        val scores = candidates.map { article ->
            val articleVector = vectors[article.id] ?: emptyMap()
            val contentScore = CosineSimilarity.cosine(articleVector, profileVector)
            val tagVector = articleTagVectors[article.id] ?: emptyMap()
            val tagScore = CosineSimilarity.cosineDense(tagVector, userTagVector)
            val recency = computeRecency(article.publishTime ?: article.createdAt ?: now, now, tauHours)
            val score = alpha * contentScore + (1 - alpha) * tagScore + beta * recency
            article to (score.coerceIn(0.0, 1.0))
        }
        val average = if (scores.isNotEmpty()) scores.sumOf { it.second } / scores.size else 0.0
        logger.debug("RecommendForUser candidates={} averageScore={}", scores.size, "%.4f".format(average))
        val sorted = scores
            .sortedByDescending { it.second }
            .take(topK)
        val tagNamesById = tagDao.findAll().associateBy({ it.id!! }, { it.name })
        return sorted.map { (article, score) ->
            ArticleSummaryDTO(
                id = article.id!!,
                title = article.title,
                summary = article.content.take(200),
                publishTime = article.publishTime,
                tags = (articleTagVectors[article.id] ?: emptyMap()).keys.mapNotNull { tagNamesById[it] },
                score = score,
            )
        }
    }

    private fun buildTfidfVectors(articles: List<Article>): Map<Long, Map<Int, Double>> {
        val contentMap = articles.associate { article ->
            article.id!! to article.content
        }
        return vectorizer.vectorize(contentMap)
    }

    private fun buildUserProfileVector(
        userId: Long,
        articleVectors: Map<Long, Map<Int, Double>>,
        historyLimit: Int = 20,
    ): Map<Int, Double> {
        val historyIds = userEventDao.findRecentArticleIdsByUser(userId, historyLimit)
        if (historyIds.isEmpty()) {
            return emptyMap()
        }
        val relevant = historyIds.mapNotNull { articleVectors[it] }
        if (relevant.isEmpty()) {
            return emptyMap()
        }
        val aggregated = mutableMapOf<Int, Double>()
        relevant.forEach { vector ->
            vector.forEach { (index, value) ->
                aggregated[index] = (aggregated[index] ?: 0.0) + value
            }
        }
        val factor = 1.0 / relevant.size
        return aggregated.mapValues { it.value * factor }
    }

    private fun buildUserTagVector(userId: Long): Map<Long, Double> {
        val tags = userTagDao.findViewsByUserId(userId)
        if (tags.isEmpty()) {
            return emptyMap()
        }
        val maxWeight = tags.maxOf { it.weight }.takeIf { it > 0 } ?: 1.0
        return tags.associate { it.tagId to it.weight / maxWeight }
    }

    private fun buildArticleTagVectors(articles: List<Article>): Map<Long, Map<Long, Double>> {
        val ids = articles.mapNotNull { it.id }
        if (ids.isEmpty()) {
            return emptyMap()
        }
        val group = articleTagDao.findByArticleIds(ids)
            .groupBy { it.articleId }
            .mapValues { (_, list) ->
                val maxWeight = list.maxOfOrNull { it.weight }?.takeIf { it > 0 } ?: 1.0
                list.associate { it.tagId to it.weight / maxWeight }
            }
        return group
    }

    private fun computeRecency(publishTime: LocalDateTime, now: LocalDateTime, tauHours: Double): Double {
        val deltaHours = Duration.between(publishTime, now).toHours().coerceAtLeast(0)
        return exp(-deltaHours / tauHours)
    }
}
