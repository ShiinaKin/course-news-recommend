package io.sakurasou.newsrecommend.controller

import io.sakurasou.newsrecommend.dto.SimpleMessageResponse
import io.sakurasou.newsrecommend.dto.UserTagsRequest
import io.sakurasou.newsrecommend.service.UserService
import io.sakurasou.newsrecommend.service.UserTagService
import io.sakurasou.newsrecommend.util.SecurityUtils
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api")
class UserController(
    private val userService: UserService,
    private val userTagService: UserTagService,
) {

    @GetMapping("/tags")
    fun listTags() = userTagService.listAllTags()

    @GetMapping("/user/profile")
    fun profile() = userService.getUserProfile(requireUserId())

    @PostMapping("/user/tags")
    fun saveTags(@RequestBody request: UserTagsRequest): SimpleMessageResponse {
        userTagService.saveUserTags(requireUserId(), request.tagIds)
        return SimpleMessageResponse("标签已更新")
    }

    private fun requireUserId(): Long {
        val username = SecurityUtils.currentUsername()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录")
        val user = userService.findByUsername(username)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录")
        return user.id ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录")
    }
}
