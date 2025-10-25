package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.dao.ArticleDAO
import io.sakurasou.newsrecommend.dao.ArticleTagDAO
import io.sakurasou.newsrecommend.dao.TagDAO
import io.sakurasou.newsrecommend.util.CosineSimilarity
import io.sakurasou.newsrecommend.util.TFIDFVectorizer
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicReference

@Service
class TitleClassifierService(
    private val articleDao: ArticleDAO,
    private val articleTagDao: ArticleTagDAO,
    private val tagDao: TagDAO,
) {

    private val logger = LoggerFactory.getLogger(TitleClassifierService::class.java)
    private val vectorizer = TFIDFVectorizer()
    private val modelRef = AtomicReference<TitleModel?>()

    @PostConstruct
    fun init() {
        reloadModel()
    }

    fun reloadModel(maxArticles: Int = DEFAULT_MAX_ARTICLES) {
        val samples = mutableMapOf<Long, String>()
        val articleTags = mutableMapOf<Long, List<Long>>()
        var offset = 0
        val batchSize = BATCH_SIZE
        while (samples.size < maxArticles) {
            val articles = articleDao.listByPublishTime(offset, batchSize)
            if (articles.isEmpty()) {
                break
            }
            val ids = articles.mapNotNull { it.id }
            if (ids.isEmpty()) {
                break
            }
            val tagGroup = articleTagDao.findByArticleIds(ids).groupBy { it.articleId }
            articles.forEach { article ->
                val id = article.id ?: return@forEach
                val title = article.title
                val tags = tagGroup[id]?.map { it.tagId }
                if (title.isNotBlank() && !tags.isNullOrEmpty()) {
                    samples[id] = title
                    articleTags[id] = tags
                }
            }
            offset += articles.size
            if (articles.size < batchSize) {
                break
            }
        }
        if (samples.isEmpty()) {
            logger.warn("TitleClassifier model not built: no articles with tags available")
            modelRef.set(null)
            return
        }
        val tfidfModel = vectorizer.fit(samples)
        val tagNames = tagDao.findAll()
            .associateBy({ it.id!! }, { it.name })
        val titleModel = TitleModel(
            tfidfModel = tfidfModel,
            articleVectors = tfidfModel.documentVectors,
            articleTags = articleTags,
            tagNames = tagNames,
        )
        modelRef.set(titleModel)
        logger.info("TitleClassifier model built with {} articles and {} tags", samples.size, tagNames.size)
    }

    fun predictTags(title: String, topK: Int = DEFAULT_TOP_K, neighborK: Int = DEFAULT_NEIGHBOR_K): List<TagPrediction> {
        if (title.isBlank()) {
            return emptyList()
        }
        val model = modelRef.get() ?: return emptyList()
        val effectiveTopK = topK.coerceIn(1, MAX_TOP_K)
        val effectiveNeighbors = neighborK.coerceIn(1, MAX_NEIGHBOR_K)
        val titleVector = model.tfidfModel.transform(title)
        if (titleVector.isEmpty()) {
            return emptyList()
        }
        val neighborScores = model.articleVectors.mapNotNull { (articleId, vector) ->
            val similarity = CosineSimilarity.cosine(vector, titleVector)
            if (similarity <= 0.0) {
                null
            } else {
                articleId to similarity
            }
        }.sortedByDescending { it.second }
        if (neighborScores.isEmpty()) {
            return emptyList()
        }
        val selectedNeighbors = neighborScores.take(effectiveNeighbors)
        val tagScores = mutableMapOf<Long, Double>()
        selectedNeighbors.forEach { (articleId, similarity) ->
            val tags = model.articleTags[articleId] ?: return@forEach
            tags.forEach { tagId ->
                tagScores[tagId] = (tagScores[tagId] ?: 0.0) + similarity
            }
        }
        if (tagScores.isEmpty()) {
            return emptyList()
        }
        return tagScores.entries
            .sortedByDescending { it.value }
            .take(effectiveTopK)
            .mapNotNull { (tagId, score) ->
                val name = model.tagNames[tagId] ?: return@mapNotNull null
                TagPrediction(tagId = tagId, tagName = name, score = score)
            }
    }

    data class TagPrediction(
        val tagId: Long,
        val tagName: String,
        val score: Double,
    )

    private data class TitleModel(
        val tfidfModel: TFIDFVectorizer.TFIDFModel,
        val articleVectors: Map<Long, Map<Int, Double>>,
        val articleTags: Map<Long, List<Long>>,
        val tagNames: Map<Long, String>,
    )

    private companion object {
        private const val BATCH_SIZE = 200
        private const val DEFAULT_MAX_ARTICLES = 1000
        private const val DEFAULT_TOP_K = 3
        private const val DEFAULT_NEIGHBOR_K = 5
        private const val MAX_TOP_K = 10
        private const val MAX_NEIGHBOR_K = 20
    }
}
