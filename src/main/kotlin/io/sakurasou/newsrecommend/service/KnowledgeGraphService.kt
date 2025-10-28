package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.dao.ArticleDAO
import io.sakurasou.newsrecommend.dao.KnowledgeEntityDAO
import io.sakurasou.newsrecommend.dao.KnowledgeRelationDAO
import io.sakurasou.newsrecommend.dao.TagDAO
import io.sakurasou.newsrecommend.dao.UserDAO
import io.sakurasou.newsrecommend.dto.KnowledgeGraphEdgeDTO
import io.sakurasou.newsrecommend.dto.KnowledgeGraphNodeDTO
import io.sakurasou.newsrecommend.dto.KnowledgeGraphResponse
import io.sakurasou.newsrecommend.model.ArticleTag
import io.sakurasou.newsrecommend.model.UserTagView
import io.sakurasou.newsrecommend.model.graph.KnowledgeEntity
import io.sakurasou.newsrecommend.model.graph.KnowledgeEntityType
import io.sakurasou.newsrecommend.model.graph.KnowledgeRelation
import io.sakurasou.newsrecommend.model.graph.KnowledgeRelationType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KnowledgeGraphService(
    private val knowledgeEntityDao: KnowledgeEntityDAO,
    private val knowledgeRelationDao: KnowledgeRelationDAO,
    private val userDao: UserDAO,
    private val articleDao: ArticleDAO,
    private val tagDao: TagDAO,
) {

    private val logger = LoggerFactory.getLogger(KnowledgeGraphService::class.java)

    @Transactional
    fun syncUserTagInterests(userId: Long, tags: List<UserTagView>) {
        if (tags.isEmpty()) {
            return
        }
        val userEntityId = ensureUserEntity(userId)
        val tagNames = loadTagNames(tags.map { it.tagId }).toMutableMap()
        tags.forEach { tag ->
            val tagName = tagNames[tag.tagId] ?: tag.tagName
            val tagEntityId = ensureTagEntity(tag.tagId, tagName)
            val normalizedWeight = tag.weight.coerceAtLeast(0.0)
            upsertUndirectedRelation(
                userEntityId,
                tagEntityId,
                KnowledgeRelationType.INTEREST,
                normalizedWeight,
                MODALITY_BEHAVIOR,
            )
        }
    }

    @Transactional
    fun syncArticleTags(articleId: Long, tags: List<ArticleTag>) {
        if (tags.isEmpty()) {
            return
        }
        val articleEntityId = ensureArticleEntity(articleId)
        val tagIds = tags.map { it.tagId }
        val tagNames = loadTagNames(tagIds)
        tags.forEach { tag ->
            val tagName = tagNames[tag.tagId]
            val tagEntityId = ensureTagEntity(tag.tagId, tagName)
            val normalizedWeight = tag.weight.coerceIn(0.0, 1.0)
            upsertUndirectedRelation(
                articleEntityId,
                tagEntityId,
                KnowledgeRelationType.ANNOTATED_WITH,
                normalizedWeight,
                MODALITY_TEXT,
            )
        }
    }

    fun loadGraphSummary(focusType: KnowledgeEntityType?): KnowledgeGraphResponse {
        val entities = knowledgeEntityDao.findAll()
        if (entities.isEmpty()) {
            return KnowledgeGraphResponse(emptyList(), emptyList(), focusType)
        }
        val relations = knowledgeRelationDao.findAll()
        val focusIds = if (focusType != null) {
            entities.filter { it.entityType == focusType }.mapNotNull { it.id }
        } else {
            emptyList()
        }
        if (focusType != null && focusIds.isEmpty()) {
            return KnowledgeGraphResponse(emptyList(), emptyList(), focusType)
        }
        val allowedIds: Set<Long> = if (focusType == null) {
            entities.mapNotNull { it.id }.toSet()
        } else {
            val neighborIds = relations
                .filter { focusIds.contains(it.sourceId) || focusIds.contains(it.targetId) }
                .flatMap { listOf(it.sourceId, it.targetId) }
            (focusIds + neighborIds).toSet()
        }
        if (allowedIds.isEmpty()) {
            return KnowledgeGraphResponse(emptyList(), emptyList(), focusType)
        }
        val nodes = entities
            .filter { entity -> entity.id != null && allowedIds.contains(entity.id) }
            .map { entity ->
                KnowledgeGraphNodeDTO(
                    id = entity.id!!,
                    label = labelForEntity(entity),
                    type = entity.entityType,
                    modality = entity.modality,
                )
            }
        val focusIdSet = focusIds.toSet()
        val edges = relations
            .asSequence()
            .filter { relation ->
                allowedIds.contains(relation.sourceId) && allowedIds.contains(relation.targetId)
            }
            .filter { relation ->
                focusType == null || focusIdSet.contains(relation.sourceId) || focusIdSet.contains(relation.targetId)
            }
            .map { relation ->
                KnowledgeGraphEdgeDTO(
                    source = relation.sourceId,
                    target = relation.targetId,
                    relationType = relation.relationType,
                    modality = relation.modality,
                    weight = relation.weight,
                )
            }
            .toList()
        return KnowledgeGraphResponse(nodes, edges, focusType)
    }

    private fun ensureUserEntity(userId: Long): Long {
        val reference = userId.toString()
        val existing = knowledgeEntityDao.findByExternalId(KnowledgeEntityType.USER, reference)
        val user = userDao.findById(userId)
        val displayName = user?.nickname ?: "user-$reference"
        if (existing != null) {
            if (user != null && existing.name != displayName) {
                val updated = existing.copy(name = displayName)
                knowledgeEntityDao.update(updated)
            }
            return existing.id!!
        }
        val entity = KnowledgeEntity(
            entityType = KnowledgeEntityType.USER,
            externalId = reference,
            name = displayName,
            description = user?.username,
            modality = null,
        )
        knowledgeEntityDao.insert(entity)
        logger.debug("Created KG entity for user {}", userId)
        return entity.id!!
    }

    private fun ensureArticleEntity(articleId: Long): Long {
        val reference = articleId.toString()
        val existing = knowledgeEntityDao.findByExternalId(KnowledgeEntityType.ARTICLE, reference)
        val article = articleDao.findById(articleId)
        val name = article?.title ?: "article-$reference"
        val description = article?.content?.take(240)
        if (existing != null) {
            if (article != null && (existing.name != name || existing.description != description)) {
                val updated = existing.copy(name = name, description = description)
                knowledgeEntityDao.update(updated)
            }
            return existing.id!!
        }
        val entity = KnowledgeEntity(
            entityType = KnowledgeEntityType.ARTICLE,
            externalId = reference,
            name = name,
            description = description,
            modality = MODALITY_TEXT,
        )
        knowledgeEntityDao.insert(entity)
        logger.debug("Created KG entity for article {}", articleId)
        return entity.id!!
    }

    private fun ensureTagEntity(tagId: Long, tagName: String?): Long {
        val reference = tagId.toString()
        val existing = knowledgeEntityDao.findByExternalId(KnowledgeEntityType.TAG, reference)
        val name = tagName ?: "tag-$reference"
        if (existing != null) {
            if (existing.name != name) {
                val updated = existing.copy(name = name)
                knowledgeEntityDao.update(updated)
            }
            return existing.id!!
        }
        val entity = KnowledgeEntity(
            entityType = KnowledgeEntityType.TAG,
            externalId = reference,
            name = name,
            modality = MODALITY_TEXT,
        )
        knowledgeEntityDao.insert(entity)
        logger.debug("Created KG entity for tag {}", tagId)
        return entity.id!!
    }

    private fun upsertUndirectedRelation(
        firstId: Long,
        secondId: Long,
        type: KnowledgeRelationType,
        weight: Double,
        modality: String,
    ) {
        val (sourceId, targetId) = if (firstId <= secondId) {
            firstId to secondId
        } else {
            secondId to firstId
        }
        val safeWeight = weight.coerceAtLeast(0.0)
        val existing = knowledgeRelationDao.findRelation(sourceId, targetId, type, modality)
        if (existing != null) {
            knowledgeRelationDao.updateWeight(existing.id!!, safeWeight)
            return
        }
        val relation = KnowledgeRelation(
            sourceId = sourceId,
            targetId = targetId,
            relationType = type,
            modality = modality,
            weight = safeWeight,
        )
        knowledgeRelationDao.insert(relation)
    }

    private fun loadTagNames(tagIds: List<Long>): Map<Long, String> {
        if (tagIds.isEmpty()) {
            return emptyMap()
        }
        val rows = tagDao.findByIds(tagIds)
        return rows.associate { row -> row.id!! to row.name }
    }

    private companion object {
        private const val MODALITY_TEXT = "text"
        private const val MODALITY_BEHAVIOR = "behavior"
    }

    private fun labelForEntity(entity: KnowledgeEntity): String {
        return entity.name?.takeIf { it.isNotBlank() }
            ?: "${entity.entityType.name.lowercase()}-${entity.id ?: entity.externalId ?: "?"}"
    }
}
