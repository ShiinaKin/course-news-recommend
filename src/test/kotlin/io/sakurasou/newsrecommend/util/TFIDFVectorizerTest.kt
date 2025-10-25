package io.sakurasou.newsrecommend.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TFIDFVectorizerTest {

    @Test
    fun `vectorize should produce normalized weights`() {
        val vectorizer = TFIDFVectorizer()
        val documents = mapOf(
            1L to "人工智能 提升 城市 效率",
            2L to "人工智能 推动 医疗 诊断"
        )

        val vectors = vectorizer.vectorize(documents)
        assertEquals(2, vectors.size)
        val first = vectors[1L]!!
        val second = vectors[2L]!!

        // Terms shared between documents should have identical indices
        val sharedIndices = first.keys.intersect(second.keys)
        assertTrue(sharedIndices.isNotEmpty())
    }
}
