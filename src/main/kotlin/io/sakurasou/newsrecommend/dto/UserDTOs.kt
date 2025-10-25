package io.sakurasou.newsrecommend.dto

data class UserProfileResponse(
    val id: Long,
    val username: String,
    val nickname: String,
    val tags: List<UserTagResponse>,
)

data class UserTagResponse(
    val id: Long,
    val name: String,
    val weight: Double,
)

data class UserTagsRequest(
    val tagIds: List<Long>,
)
