package com.lumos.lumosspring.stock.repository

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class TeamQueryRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {

    fun getTeamIdByUserId(userId: UUID): Long? {
        val params = mapOf("userId" to userId)

        return try {
            jdbcTemplate.queryForObject("""
                SELECT id_team from team
                where driver_id =:userId or electrician_id = :userId
            """.trimIndent(), params, Long::class.java)
        } catch (_: EmptyResultDataAccessException) {
            null
        }
    }

}