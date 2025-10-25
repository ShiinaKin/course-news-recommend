package io.sakurasou.newsrecommend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val reco: RecommendationProperties = RecommendationProperties(),
    val stt: STTProperties = STTProperties(),
    val ocr: OCRProperties = OCRProperties(),
)

data class RecommendationProperties(
    val alpha: Double = 0.5,
    val beta: Double = 0.05,
    val recentDays: Int = 14,
    val candidateLimit: Int = 1000,
)

data class STTProperties(
    val mode: String = "mock",
    val modelPath: String = "",
    val binaryPath: String = "",
    val language: String = "",
)

data class OCRProperties(
    val binPath: String = "./tools/macocr/.build/release/macocr",
    val langs: String = "zh-Hans,en",
    val fast: Boolean = true,
)
