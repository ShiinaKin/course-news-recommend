package io.sakurasou.newsrecommend.dao

import io.sakurasou.newsrecommend.model.UserEvent
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface UserEventDAO {
    fun insert(event: UserEvent): Int
    fun findRecentArticleIdsByUser(
        @Param("userId") userId: Long,
        @Param("limit") limit: Int,
    ): List<Long>
}
