package io.sakurasou.newsrecommend.dao

import io.sakurasou.newsrecommend.model.Article
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface ArticleDAO {
    fun findById(@Param("id") id: Long): Article?
    fun findByTitle(@Param("title") title: String): Article?
    fun countAll(): Long
    fun listByPublishTime(@Param("offset") offset: Int, @Param("limit") limit: Int): List<Article>
    fun listByCreatedAt(@Param("offset") offset: Int, @Param("limit") limit: Int): List<Article>
    fun findRecent(@Param("limit") limit: Int): List<Article>
    fun insert(article: Article): Int
}
