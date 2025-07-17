package com.lumos.lumosspring.util

import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

object Utils {
    fun getCurrentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
        return UUID.fromString(authentication.name)
    }
}