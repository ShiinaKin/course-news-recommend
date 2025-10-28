package io.sakurasou.newsrecommend.controller

import io.sakurasou.newsrecommend.dto.KnowledgeGraphResponse
import io.sakurasou.newsrecommend.model.graph.KnowledgeEntityType
import io.sakurasou.newsrecommend.service.KnowledgeGraphService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/graph")
class KnowledgeGraphController(
    private val knowledgeGraphService: KnowledgeGraphService,
) {

    @GetMapping
    fun summary(@RequestParam(name = "focusType", required = false) focusType: String?): KnowledgeGraphResponse {
        val focus = focusType?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
            runCatching { KnowledgeEntityType.valueOf(raw.uppercase()) }
                .getOrElse {
                    throw ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unknown focusType: $focusType",
                    )
                }
        }
        return knowledgeGraphService.loadGraphSummary(focus)
    }
}
