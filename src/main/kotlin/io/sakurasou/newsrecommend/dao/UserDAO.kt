package io.sakurasou.newsrecommend.dao

import io.sakurasou.newsrecommend.model.User
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface UserDAO {
    fun findById(@Param("id") id: Long): User?
    fun findByUsername(@Param("username") username: String): User?
    fun insert(user: User): Int
}
