package io.sakurasou.newsrecommend.service

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import io.sakurasou.newsrecommend.dao.ArticleDAO
import io.sakurasou.newsrecommend.dao.ArticleTagDAO
import io.sakurasou.newsrecommend.service.TitleClassifierService.TagPrediction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import com.ninjasquad.springmockk.MockkBean
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RSSImportServiceIntegrationTest {

    @Autowired
    private lateinit var rssImportService: RSSImportService

    @Autowired
    private lateinit var articleDao: ArticleDAO

    @Autowired
    private lateinit var articleTagDao: ArticleTagDAO

    @MockkBean
    private lateinit var rssFeedClient: RSSFeedClient

    @MockkBean
    private lateinit var titleClassifierService: TitleClassifierService

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `import feed persists new articles and applies predicted tags`() {
        val feed = RSSFeedResult(
            channel = null,
            items = listOf(
                RssItemResult(
                    title = "HarmonyOS 发布会速览",
                    link = "https://example.com/harmonyos",
                    description = "华为在发布会上公布了 HarmonyOS 6 的主要更新",
                    author = "News Team",
                    pubDate = "Fri, 24 Oct 2025 18:25:27 +0800",
                ),
            ),
        )
        every { rssFeedClient.fetch(any()) } returns feed
        every { titleClassifierService.predictTags(any(), any(), any()) } returns listOf(
            TagPrediction(tagId = 1L, tagName = "科技", score = 0.9),
            TagPrediction(tagId = 4L, tagName = "国际", score = 0.3),
        )
        every { titleClassifierService.reloadModel() } just Runs

        val initialCount = articleDao.countAll()
        val imported = rssImportService.import("https://example.com/feed")

        assertEquals(1, imported)
        assertEquals(initialCount + 1, articleDao.countAll())

        val article = articleDao.findByTitle("HarmonyOS 发布会速览")
        assertNotNull(article)
        val tags = articleTagDao.findByArticleId(article.id!!)
        assertTrue(tags.any { it.tagId == 1L })

        verify(exactly = 1) { rssFeedClient.fetch("https://example.com/feed") }
        verify { titleClassifierService.predictTags(match { it.contains("HarmonyOS") }, any(), any()) }
        verify { titleClassifierService.reloadModel() }
    }
}
