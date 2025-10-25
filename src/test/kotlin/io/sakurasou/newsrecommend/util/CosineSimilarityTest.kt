package io.sakurasou.newsrecommend.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CosineSimilarityTest {

    @Test
    fun `cosine returns 1 for identical vectors`() {
        val a = mapOf(1 to 0.5, 2 to 0.5)
        val b = mapOf(1 to 0.5, 2 to 0.5)
        assertEquals(1.0, CosineSimilarity.cosine(a, b), 1e-6)
    }

    @Test
    fun `cosine dense handles sparse overlap`() {
        val a = mapOf(1L to 0.8, 2L to 0.2)
        val b = mapOf(1L to 0.4, 3L to 0.9)
        val score = CosineSimilarity.cosineDense(a, b)
        assertTrue(score in 0.0..1.0)
    }
}
