package io.sakurasou.newsrecommend.dto

import io.sakurasou.newsrecommend.model.graph.KnowledgeEntityType
import io.sakurasou.newsrecommend.model.graph.KnowledgeRelationType

data class KnowledgeGraphNodeDTO(
    val id: Long,
    val label: String,
    val type: KnowledgeEntityType,
    val modality: String? = null,
)

data class KnowledgeGraphEdgeDTO(
    val source: Long,
    val target: Long,
    val relationType: KnowledgeRelationType,
    val modality: String? = null,
    val weight: Double,
)

data class KnowledgeGraphResponse(
    val nodes: List<KnowledgeGraphNodeDTO>,
    val edges: List<KnowledgeGraphEdgeDTO>,
    val focusType: KnowledgeEntityType? = null,
)
