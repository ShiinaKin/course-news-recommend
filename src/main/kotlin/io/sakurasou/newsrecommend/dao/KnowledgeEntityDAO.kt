package io.sakurasou.newsrecommend.dao

import io.sakurasou.newsrecommend.model.graph.KnowledgeEntity
import io.sakurasou.newsrecommend.model.graph.KnowledgeEntityType
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface KnowledgeEntityDAO {
    fun findById(@Param("id") id: Long): KnowledgeEntity?

    fun findByExternalId(
        @Param("entityType") entityType: KnowledgeEntityType,
        @Param("externalId") externalId: String,
    ): KnowledgeEntity?

    fun findAll(): List<KnowledgeEntity>

    fun findByType(@Param("entityType") entityType: KnowledgeEntityType): List<KnowledgeEntity>

    fun insert(entity: KnowledgeEntity): Int

    fun update(entity: KnowledgeEntity): Int
}
