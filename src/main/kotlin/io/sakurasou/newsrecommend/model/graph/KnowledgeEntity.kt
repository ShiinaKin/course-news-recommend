package io.sakurasou.newsrecommend.model.graph

import java.time.LocalDateTime

data class KnowledgeEntity(
    val id: Long? = null,
    val entityType: KnowledgeEntityType,
    val externalId: String? = null,
    val name: String? = null,
    val description: String? = null,
    val modality: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)

enum class KnowledgeEntityType {
    USER,
    ARTICLE,
    TAG,
    PERSON,
    ORGANIZATION,
    LOCATION,
    EVENT,
    MEDIA_OBJECT,
    OTHER,
}
