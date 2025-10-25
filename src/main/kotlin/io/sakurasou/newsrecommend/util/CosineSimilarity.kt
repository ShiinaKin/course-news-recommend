package io.sakurasou.newsrecommend.util

import kotlin.math.sqrt

object CosineSimilarity {

    fun cosine(a: Map<Int, Double>, b: Map<Int, Double>): Double {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0
        }
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for ((index, value) in a) {
            normA += value * value
            val other = b[index]
            if (other != null) {
                dot += value * other
            }
        }
        for (value in b.values) {
            normB += value * value
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0
        }
        return dot / (sqrt(normA) * sqrt(normB))
    }

    fun cosineDense(a: Map<Long, Double>, b: Map<Long, Double>): Double {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0
        }
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for ((key, value) in a) {
            normA += value * value
            val other = b[key]
            if (other != null) {
                dot += value * other
            }
        }
        for (value in b.values) {
            normB += value * value
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0
        }
        return dot / (sqrt(normA) * sqrt(normB))
    }
}
