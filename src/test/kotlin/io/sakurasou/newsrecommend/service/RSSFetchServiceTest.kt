package io.sakurasou.newsrecommend.service

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RSSFetchServiceTest {

    private val xmlMapper = XmlMapper().registerKotlinModule()

    @Test
    fun `parse sample feed with jackson`() {
        val xml = javaClass.getResource("/rss/feed.rss")!!.readText()
        val root = xmlMapper.readValue(xml, RssXmlRoot::class.java)

        val channel = root.channel
        assertNotNull(channel)
        assertEquals("少数派", channel.title)
        assertEquals("zh-CN", channel.language)
        assertEquals(10, channel.items.size)

        val first = channel.items.first()
        assertEquals("鸿蒙 1024 程序员节征文获奖结果公布", first.title)
        assertEquals("少数派编辑部", first.author)
    }
}
