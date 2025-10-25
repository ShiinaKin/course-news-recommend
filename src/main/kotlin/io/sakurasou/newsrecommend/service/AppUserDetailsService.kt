package io.sakurasou.newsrecommend.service

import io.sakurasou.newsrecommend.dao.UserDAO
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class AppUserDetailsService(
    private val userDao: UserDAO,
) : UserDetailsService {

    private val defaultAuthorities: Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority("ROLE_USER"))

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userDao.findByUsername(username)
            ?: throw UsernameNotFoundException("User $username not found")
        return org.springframework.security.core.userdetails.User
            .withUsername(user.username)
            .password(user.passwordHash)
            .authorities(defaultAuthorities)
            .accountLocked(false)
            .disabled(false)
            .build()
    }
}
