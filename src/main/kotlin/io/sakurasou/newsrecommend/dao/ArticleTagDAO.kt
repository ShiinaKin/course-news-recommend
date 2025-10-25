package io.sakurasou.newsrecommend.dao

import io.sakurasou.newsrecommend.model.ArticleTag
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface ArticleTagDAO {
    fun findByArticleId(@Param("articleId") articleId: Long): List<ArticleTag>
    fun findByArticleIds(@Param("articleIds") articleIds: List<Long>): List<ArticleTag>
    fun deleteByArticleId(@Param("articleId") articleId: Long): Int
    fun insert(articleTag: ArticleTag): Int
}
