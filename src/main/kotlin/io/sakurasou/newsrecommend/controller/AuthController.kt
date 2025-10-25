package io.sakurasou.newsrecommend.controller

import io.sakurasou.newsrecommend.dto.LoginRequest
import io.sakurasou.newsrecommend.dto.RegisterRequest
import io.sakurasou.newsrecommend.dto.SimpleMessageResponse
import io.sakurasou.newsrecommend.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val userService: UserService,
    private val securityContextRepository: SecurityContextRepository,
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): SimpleMessageResponse {
        userService.register(request.username, request.password, request.nickname)
        return SimpleMessageResponse("注册成功")
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse,
    ): SimpleMessageResponse {
        val authenticationToken = UsernamePasswordAuthenticationToken(request.username, request.password)
        val authentication = authenticationManager.authenticate(authenticationToken)
        val context = SecurityContextHolder.createEmptyContext().apply {
            this.authentication = authentication
        }
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, servletRequest, servletResponse)
        return SimpleMessageResponse("登录成功")
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse): SimpleMessageResponse {
        val auth = SecurityContextHolder.getContext().authentication
        SecurityContextLogoutHandler().logout(request, response, auth)
        return SimpleMessageResponse("已退出登录")
    }
}
