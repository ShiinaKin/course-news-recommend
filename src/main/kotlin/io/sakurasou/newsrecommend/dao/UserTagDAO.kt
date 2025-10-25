package io.sakurasou.newsrecommend.dao

import io.sakurasou.newsrecommend.model.UserTagView
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface UserTagDAO {
    fun findViewsByUserId(@Param("userId") userId: Long): List<UserTagView>
    fun deleteByUserId(@Param("userId") userId: Long): Int
    fun insertUserTag(
        @Param("userId") userId: Long,
        @Param("tagId") tagId: Long,
        @Param("weight") weight: Double,
    ): Int

    fun incrementUserTag(
        @Param("userId") userId: Long,
        @Param("tagId") tagId: Long,
        @Param("delta") delta: Double,
        @Param("maxWeight") maxWeight: Double,
    ): Int
}
