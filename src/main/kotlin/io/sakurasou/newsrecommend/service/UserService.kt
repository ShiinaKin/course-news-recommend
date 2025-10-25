package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.dao.TagDAO
import io.sakurasou.newsrecommend.dao.UserDAO
import io.sakurasou.newsrecommend.dao.UserTagDAO
import io.sakurasou.newsrecommend.dto.UserProfileResponse
import io.sakurasou.newsrecommend.dto.UserTagResponse
import io.sakurasou.newsrecommend.model.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userDao: UserDAO,
    private val userTagDao: UserTagDAO,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun register(username: String, rawPassword: String, nickname: String): User {
        val existing = userDao.findByUsername(username)
        if (existing != null) {
            throw IllegalArgumentException("用户名已存在")
        }
        val encodedPassword = passwordEncoder.encode(rawPassword)
        val user = User(
            username = username,
            passwordHash = encodedPassword,
            nickname = nickname,
        )
        userDao.insert(user)
        return userDao.findByUsername(username) ?: user
    }

    fun findByUsername(username: String): User? = userDao.findByUsername(username)

    fun findById(id: Long): User? = userDao.findById(id)

    fun getUserProfile(userId: Long): UserProfileResponse {
        val user = userDao.findById(userId) ?: throw IllegalArgumentException("用户不存在")
        val tags = userTagDao.findViewsByUserId(userId)
        val responses = tags.map { UserTagResponse(id = it.tagId, name = it.tagName, weight = it.weight) }
        return UserProfileResponse(
            id = user.id!!,
            username = user.username,
            nickname = user.nickname,
            tags = responses,
        )
    }
}
