package io.sakurasou.newsrecommend.controller

import io.sakurasou.newsrecommend.dto.RecommendResponse
import io.sakurasou.newsrecommend.service.RecommendService
import io.sakurasou.newsrecommend.service.UserService
import io.sakurasou.newsrecommend.util.SecurityUtils
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class RecommendController(
    private val recommendService: RecommendService,
    private val userService: UserService,
) {

    @GetMapping("/api/recommend")
    fun recommend(@RequestParam(defaultValue = "10") topK: Int): RecommendResponse {
        val safeTopK = topK.coerceIn(1, 50)
        val username = SecurityUtils.currentUsername()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录用户无法获取推荐")
        val user = userService.findByUsername(username)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录用户无法获取推荐")
        val items = recommendService.recommendForUser(user.id!!, safeTopK)
        return RecommendResponse(items)
    }
}
