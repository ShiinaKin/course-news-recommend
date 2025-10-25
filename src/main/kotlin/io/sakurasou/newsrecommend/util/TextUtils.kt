package io.sakurasou.newsrecommend.util

object TextUtils {
    private val splitter = Regex("[^\\p{IsLetter}\\p{IsDigit}\\p{IsHan}]+")
    private val stopWords = setOf("的", "了", "和", "是", "在", "就", "与", "及", "为", "对", "a", "the", "and", "of")

    fun tokenize(text: String): List<String> {
        if (text.isBlank()) {
            return emptyList()
        }
        return text
            .lowercase()
            .split(splitter)
            .map { it.trim() }
            .filter { it.isNotEmpty() && it !in stopWords }
    }
}
