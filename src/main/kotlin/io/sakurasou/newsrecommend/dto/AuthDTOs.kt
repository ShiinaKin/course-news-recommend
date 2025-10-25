package io.sakurasou.newsrecommend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank
    @field:Size(min = 3, max = 32)
    val username: String,
    @field:NotBlank
    @field:Size(min = 6, max = 64)
    val password: String,
    @field:NotBlank
    @field:Size(min = 2, max = 32)
    val nickname: String,
)

data class LoginRequest(
    @field:NotBlank
    val username: String,
    @field:NotBlank
    val password: String,
)

data class SimpleMessageResponse(
    val message: String,
)
