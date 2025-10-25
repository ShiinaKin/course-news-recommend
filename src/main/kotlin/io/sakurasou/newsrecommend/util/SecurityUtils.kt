package io.sakurasou.newsrecommend.util

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

object SecurityUtils {
    fun currentAuthentication(): Authentication? = SecurityContextHolder.getContext().authentication

    fun currentUsername(): String? {
        val auth = currentAuthentication() ?: return null
        if (!auth.isAuthenticated || auth.name == "anonymousUser") {
            return null
        }
        return auth.name
    }
}
