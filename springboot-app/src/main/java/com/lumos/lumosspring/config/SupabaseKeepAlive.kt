package com.lumos.lumosspring.config

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SupabaseKeepAlive(
    private val jdbcTemplate: JdbcTemplate
) {
    @Scheduled(fixedDelay = 300000)
    @Transactional(readOnly = true)
    fun keepDbAlive() {
        try {
            jdbcTemplate.queryForObject<Int>("select 1")
        } catch(_: Exception){
        }
    }
}