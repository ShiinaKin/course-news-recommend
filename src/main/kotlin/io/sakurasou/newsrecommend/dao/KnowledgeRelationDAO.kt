package io.sakurasou.newsrecommend.dao

import io.sakurasou.newsrecommend.model.graph.KnowledgeRelation
import io.sakurasou.newsrecommend.model.graph.KnowledgeRelationType
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface KnowledgeRelationDAO {
    fun findRelation(
        @Param("sourceId") sourceId: Long,
        @Param("targetId") targetId: Long,
        @Param("relationType") relationType: KnowledgeRelationType,
        @Param("modality") modality: String,
    ): KnowledgeRelation?

    fun findAll(): List<KnowledgeRelation>

    fun insert(relation: KnowledgeRelation): Int

    fun updateWeight(
        @Param("id") id: Long,
        @Param("weight") weight: Double,
    ): Int
}
