package io.sakurasou.newsrecommend.dao

import io.sakurasou.newsrecommend.model.MediaJob
import io.sakurasou.newsrecommend.model.MediaJobStatus
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface MediaJobDAO {
    fun insert(job: MediaJob): Int
    fun updateStatus(
        @Param("id") id: Long,
        @Param("status") status: MediaJobStatus,
        @Param("resultText") resultText: String?,
    ): Int

    fun updateRunning(@Param("id") id: Long, @Param("status") status: MediaJobStatus): Int
    fun findById(@Param("id") id: Long): MediaJob?
}
