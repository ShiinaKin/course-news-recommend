package io.sakurasou.newsrecommend.config

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RSSClientConfig {

    @Bean
    fun rssXmlMapper(): XmlMapper = XmlMapper().apply { this.registerModule(JavaTimeModule()).registerKotlinModule() }

    @Bean(destroyMethod = "close")
    fun rssHttpClient(): HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 10_000
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, "news-recommend/1.0 (+https://news-recommend.local)")
            header(
                HttpHeaders.Accept,
                "application/rss+xml, application/xml;q=0.9, text/xml;q=0.8, */*;q=0.5",
            )
        }
    }
}
