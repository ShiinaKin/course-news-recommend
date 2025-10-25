package io.sakurasou.newsrecommend.dao

import io.sakurasou.newsrecommend.model.Tag
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface TagDAO {
    fun findAll(): List<Tag>
    fun findByIds(@Param("ids") ids: List<Long>): List<Tag>
}
