package io.sakurasou.newsrecommend.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.net.URI

@Service
class RSSFetchService(
    private val rssHttpClient: HttpClient,
    @field:Qualifier("rssXmlMapper") private val xmlMapper: XmlMapper,
): RSSFeedClient {

    override fun fetch(url: String): RSSFeedResult {
        val targetUri = validateAndNormalizeUrl(url)
        return runBlocking {
            val response = executeRequest(targetUri)
            if (!response.status.isSuccess()) {
                throw RssFetchException(
                    message = "RSS 源请求失败，HTTP 状态码：${response.status.value}",
                    status = HttpStatus.BAD_GATEWAY,
                )
            }
            val body = response.bodyAsText()
            val feed = parseRss(body)
            RSSFeedResult(
                channel = feed.channel?.toDomain(),
                items = feed.channel?.items?.map { it.toDomain() } ?: emptyList(),
            )
        }
    }

    private fun parseRss(xml: String): RssXmlRoot =
        try {
            xmlMapper.readValue(xml, RssXmlRoot::class.java)
        } catch (ex: Exception) {
            throw RssFetchException("RSS 内容解析失败", HttpStatus.BAD_GATEWAY, ex)
        }

    private suspend fun executeRequest(uri: URI): HttpResponse {
        val hostHeader = buildHostHeader(uri)
        val originHeader = buildOriginHeader(uri)
        return rssHttpClient.get(uri.toString()) {
            header(HttpHeaders.Host, hostHeader)
            header(HttpHeaders.Origin, originHeader)
            header(HttpHeaders.Referrer, originHeader)
        }
    }

    private fun validateAndNormalizeUrl(url: String): URI {
        val uri = try {
            URI(url.trim())
        } catch (ex: IllegalArgumentException) {
            throw RssFetchException("无效的 RSS 链接", HttpStatus.BAD_REQUEST, ex)
        }
        if (!uri.isAbsolute || uri.scheme.isNullOrBlank()) {
            throw RssFetchException("RSS 链接必须使用绝对路径", HttpStatus.BAD_REQUEST)
        }
        if (uri.scheme !in allowedSchemes) {
            throw RssFetchException("暂不支持的协议：${uri.scheme}", HttpStatus.BAD_REQUEST)
        }
        if (uri.host.isNullOrBlank()) {
            throw RssFetchException("RSS 链接缺少主机名", HttpStatus.BAD_REQUEST)
        }
        return uri
    }

    private fun buildHostHeader(uri: URI): String {
        val defaultPort = when (uri.scheme) {
            "http" -> 80
            "https" -> 443
            else -> -1
        }
        return when {
            uri.port == -1 || uri.port == defaultPort -> uri.host
            else -> "${uri.host}:${uri.port}"
        }
    }

    private fun buildOriginHeader(uri: URI): String {
        val port = when (uri.port) {
            -1 -> ""
            else -> ":${uri.port}"
        }
        return "${uri.scheme}://${uri.host}$port"
    }

    private companion object {
        private val allowedSchemes = setOf("http", "https")
    }
}

data class RSSFeedResult(
    val channel: RssChannelResult?,
    val items: List<RssItemResult>,
)

data class RssChannelResult(
    val title: String?,
    val link: String?,
    val description: String?,
    val language: String?,
    val lastUpdated: String?,
)

data class RssItemResult(
    val title: String?,
    val link: String?,
    val description: String?,
    val author: String?,
    val pubDate: String?,
)

class RssFetchException(
    message: String,
    val status: HttpStatus,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

@JacksonXmlRootElement(localName = "rss")
@JsonIgnoreProperties(ignoreUnknown = true)
data class RssXmlRoot(
    @JacksonXmlProperty(localName = "channel")
    val channel: RssXmlChannel? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RssXmlChannel(
    val title: String? = null,
    val link: String? = null,
    val description: String? = null,
    val language: String? = null,
    @JacksonXmlProperty(localName = "pubDate")
    val pubDate: String? = null,
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "item")
    val items: List<RssXmlItem> = emptyList(),
) {
    fun toDomain(): RssChannelResult =
        RssChannelResult(
            title = title,
            link = link,
            description = description,
            language = language,
            lastUpdated = pubDate,
        )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class RssXmlItem(
    val title: String? = null,
    val link: String? = null,
    val description: String? = null,
    val author: String? = null,
    @JacksonXmlProperty(localName = "pubDate")
    val pubDate: String? = null,
) {
    fun toDomain(): RssItemResult =
        RssItemResult(
            title = title,
            link = link,
            description = description,
            author = author,
            pubDate = pubDate,
        )
}
