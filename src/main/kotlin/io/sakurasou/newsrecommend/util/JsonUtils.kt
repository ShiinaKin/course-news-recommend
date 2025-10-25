package io.sakurasou.newsrecommend.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object JsonUtils {
    private val mapper = jacksonObjectMapper()

    fun parseTree(json: String): JsonNode = mapper.readTree(json)
}
