package io.sakurasou.newsrecommend.service

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.sakurasou.newsrecommend.dao.ArticleTagDAO
import io.sakurasou.newsrecommend.model.ArticleTag
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class TaggingService(
    private val resourceLoader: ResourceLoader,
    private val articleTagDao: ArticleTagDAO,
) {

    private val logger = LoggerFactory.getLogger(TaggingService::class.java)
    private val yamlMapper = YAMLMapper()
    private var rules: List<TagRule> = emptyList()

    @PostConstruct
    fun loadRules() {
        try {
            val resource = resourceLoader.getResource("classpath:tags.yml")
            resource.inputStream.use { input ->
                val tree = yamlMapper.readTree(input)
                val loaded = tree["rules"]?.mapNotNull { node ->
                    val keyword = node["keyword"]?.asText()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val tagId = node["tagId"]?.asLong() ?: return@mapNotNull null
                    val weight = node["weight"]?.asDouble() ?: 0.5
                    TagRule(keyword.lowercase(), tagId, weight)
                } ?: emptyList()
                rules = loaded
                logger.info("Loaded {} tagging rules", rules.size)
            }
        } catch (ex: IOException) {
            logger.error("Failed to load tags.yml", ex)
            rules = emptyList()
        }
    }

    fun applyTags(articleId: Long, content: String) {
        if (rules.isEmpty()) {
            return
        }
        val matches = mutableMapOf<Long, Double>()
        val lower = content.lowercase()
        for (rule in rules) {
            if (lower.contains(rule.keyword)) {
                val existing = matches[rule.tagId] ?: 0.0
                matches[rule.tagId] = maxOf(existing, rule.weight)
            }
        }
        articleTagDao.deleteByArticleId(articleId)
        matches.forEach { (tagId, weight) ->
            articleTagDao.insert(ArticleTag(articleId = articleId, tagId = tagId, weight = weight))
        }
    }

    data class TagRule(
        val keyword: String,
        val tagId: Long,
        val weight: Double,
    )
}
