package io.sakurasou.newsrecommend.service

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.clearMocks
import io.mockk.every
import io.sakurasou.newsrecommend.dao.ArticleDAO
import io.sakurasou.newsrecommend.dao.ArticleTagDAO
import io.sakurasou.newsrecommend.dao.TagDAO
import io.sakurasou.newsrecommend.model.Article
import io.sakurasou.newsrecommend.model.ArticleTag
import io.sakurasou.newsrecommend.model.Tag
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TitleClassifierServiceMockkTest {

    @MockK
    lateinit var articleDao: ArticleDAO

    @MockK
    lateinit var articleTagDao: ArticleTagDAO

    @MockK
    lateinit var tagDao: TagDAO

    private lateinit var service: TitleClassifierService

    @BeforeEach
    fun init() {
        MockKAnnotations.init(this)
        service = TitleClassifierService(articleDao, articleTagDao, tagDao)
    }

    @AfterEach
    fun tearDown() {
        clearMocks(articleDao, articleTagDao, tagDao)
    }

    @Test
    fun `predictTags returns expected label for similar title`() {
        val articles = listOf(
            Article(id = 1, title = "派早报：华为发布 HarmonyOS 6 等新品", content = "news", publishTime = nowMinusDays(1)),
            Article(id = 2, title = "CMF Headphone Pro 无线降噪耳机体验测评", content = "review", publishTime = nowMinusDays(2)),
            Article(id = 3, title = "越南 10 天旅行指南：吃喝玩乐全攻略", content = "travel", publishTime = nowMinusDays(3)),
        )
        stubArticles(articles)
        stubTags(
            tagsByArticle = mapOf(
                1L to listOf(ArticleTag(articleId = 1, tagId = 1, weight = 1.0)),
                2L to listOf(ArticleTag(articleId = 2, tagId = 2, weight = 1.0)),
                3L to listOf(ArticleTag(articleId = 3, tagId = 3, weight = 1.0)),
            ),
            tags = listOf(
                Tag(id = 1, name = "资讯"),
                Tag(id = 2, name = "测评"),
                Tag(id = 3, name = "旅行"),
            ),
        )

        service.reloadModel()

        val predictions = service.predictTags("HarmonyOS 发布会速览与新品汇总", topK = 2)
        assertTrue(predictions.isNotEmpty(), "should return predictions")
        val top = predictions.first()
        assertEquals(1L, top.tagId)
        assertEquals("资讯", top.tagName)
        assertTrue(top.score > 0.0)
    }

    @Test
    fun `predictTags returns empty for unknown vocabulary`() {
        val articles = listOf(
            Article(id = 1, title = "派早报：华为发布 HarmonyOS 6 等新品", content = "news", publishTime = nowMinusDays(1)),
        )
        stubArticles(articles)
        stubTags(
            tagsByArticle = mapOf(1L to listOf(ArticleTag(articleId = 1, tagId = 1, weight = 1.0))),
            tags = listOf(Tag(id = 1, name = "资讯")),
        )

        service.reloadModel()

        val predictions = service.predictTags("未知关键词不在词汇表", topK = 3)
        assertTrue(predictions.isEmpty())
    }

    private fun stubArticles(articles: List<Article>) {
        every { articleDao.listByPublishTime(any(), any()) } answers { call ->
            val offset = call.invocation.args[0] as Int
            val limit = call.invocation.args[1] as Int
            articles
                .sortedByDescending { it.publishTime ?: it.createdAt ?: LocalDateTime.MIN }
                .drop(offset)
                .take(limit)
        }
        every { articleDao.listByCreatedAt(any(), any()) } answers { call ->
            val offset = call.invocation.args[0] as Int
            val limit = call.invocation.args[1] as Int
            articles
                .sortedByDescending { it.createdAt ?: it.publishTime ?: LocalDateTime.MIN }
                .drop(offset)
                .take(limit)
        }
        every { articleDao.findRecent(any()) } answers { call ->
            val limit = call.invocation.args[0] as Int
            articles.take(limit)
        }
        every { articleDao.countAll() } returns articles.size.toLong()
        every { articleDao.findById(any()) } answers { call ->
            val id = call.invocation.args[0] as Long
            articles.firstOrNull { it.id == id }
        }
        }

    private fun stubTags(tagsByArticle: Map<Long, List<ArticleTag>>, tags: List<Tag>) {
        every { articleTagDao.findByArticleIds(any()) } answers { call ->
            val ids = call.invocation.args[0] as List<*>
            ids.flatMap { id -> tagsByArticle[id] ?: emptyList() }
        }
        every { articleTagDao.findByArticleId(any()) } answers { call ->
            val id = call.invocation.args[0] as Long
            tagsByArticle[id] ?: emptyList()
        }
        every { tagDao.findAll() } returns tags
        every { tagDao.findByIds(any()) } answers { call ->
            val ids = call.invocation.args[0] as List<*>
            tags.filter { it.id in ids }
        }
    }

    private companion object {
        fun nowMinusDays(days: Long): LocalDateTime = LocalDateTime.now().minusDays(days)
    }
}
