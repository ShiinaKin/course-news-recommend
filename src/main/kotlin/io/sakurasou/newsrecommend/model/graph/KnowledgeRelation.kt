package io.sakurasou.newsrecommend.model.graph

import java.time.LocalDateTime

data class KnowledgeRelation(
    val id: Long? = null,
    val sourceId: Long,
    val targetId: Long,
    val relationType: KnowledgeRelationType,
    val modality: String? = null,
    val weight: Double,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)

enum class KnowledgeRelationType {
    INTEREST,          // user -> tag
    ANNOTATED_WITH,    // article -> tag
    GENERATED_FROM,    // article -> media object (OCR/STT)
    RELATED_TO,        // general association
    CO_OCCURS,         // tags or entities co-appearing
    SIMILAR_TO,
}
