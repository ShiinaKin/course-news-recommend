package io.sakurasou.newsrecommend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class NewsRecommendApplication

fun main(args: Array<String>) {
    runApplication<NewsRecommendApplication>(*args)
}
