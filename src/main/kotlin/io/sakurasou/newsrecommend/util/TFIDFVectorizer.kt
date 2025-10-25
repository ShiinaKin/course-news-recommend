package io.sakurasou.newsrecommend.util

import kotlin.math.ln

class TFIDFVectorizer {

    fun vectorize(contents: Map<Long, String>): Map<Long, Map<Int, Double>> {
        if (contents.isEmpty()) {
            return emptyMap()
        }
        return fit(contents).documentVectors
    }

    fun fit(contents: Map<Long, String>): TFIDFModel {
        if (contents.isEmpty()) {
            return TFIDFModel(emptyMap(), emptyMap(), emptyMap())
        }
        val vocabulary = mutableMapOf<String, Int>()
        val documentFrequency = mutableMapOf<Int, Int>()
        val termFrequencies = mutableMapOf<Long, MutableMap<Int, Double>>()

        var nextIndex = 0
        contents.forEach { (docId, rawContent) ->
            val tokens = TextUtils.tokenize(rawContent)
            if (tokens.isEmpty()) {
                termFrequencies[docId] = mutableMapOf()
                return@forEach
            }
            val counts = mutableMapOf<Int, Double>()
            tokens.forEach { token ->
                val index = vocabulary.getOrPut(token) { nextIndex++ }
                counts[index] = (counts[index] ?: 0.0) + 1.0
            }
            val tokenCount = tokens.size.toDouble()
            val tfMap = counts.mapValues { (_, freq) -> freq / tokenCount }.toMutableMap()
            termFrequencies[docId] = tfMap
            counts.keys.forEach { idx ->
                documentFrequency[idx] = (documentFrequency[idx] ?: 0) + 1
            }
        }

        val totalDocs = contents.size
        val idf = mutableMapOf<Int, Double>()
        documentFrequency.forEach { (index, df) ->
            val value = ln((totalDocs + 1.0) / (df + 1.0)) + 1.0
            idf[index] = value
        }

        val documentVectors = mutableMapOf<Long, Map<Int, Double>>()
        termFrequencies.forEach { (docId, tfMap) ->
            val tfidf = mutableMapOf<Int, Double>()
            tfMap.forEach { (index, tf) ->
                val idfValue = idf[index] ?: 1.0
                tfidf[index] = tf * idfValue
            }
            documentVectors[docId] = tfidf
        }

        return TFIDFModel(
            vocabulary = vocabulary.toMap(),
            idf = idf.toMap(),
            documentVectors = documentVectors.toMap(),
        )
    }

    data class TFIDFModel(
        val vocabulary: Map<String, Int>,
        val idf: Map<Int, Double>,
        val documentVectors: Map<Long, Map<Int, Double>>,
    ) {
        fun transform(text: String): Map<Int, Double> {
            if (text.isBlank() || vocabulary.isEmpty()) {
                return emptyMap()
            }
            val tokens = TextUtils.tokenize(text)
            if (tokens.isEmpty()) {
                return emptyMap()
            }
            val counts = mutableMapOf<Int, Int>()
            tokens.forEach { token ->
                val index = vocabulary[token] ?: return@forEach
                counts[index] = (counts[index] ?: 0) + 1
            }
            if (counts.isEmpty()) {
                return emptyMap()
            }
            val totalTokens = tokens.size.toDouble()
            val vector = mutableMapOf<Int, Double>()
            counts.forEach { (index, count) ->
                val tf = count / totalTokens
                val idfValue = idf[index] ?: DEFAULT_IDF
                vector[index] = tf * idfValue
            }
            return vector
        }

        private companion object {
            private const val DEFAULT_IDF = 1.0
        }
    }
}
