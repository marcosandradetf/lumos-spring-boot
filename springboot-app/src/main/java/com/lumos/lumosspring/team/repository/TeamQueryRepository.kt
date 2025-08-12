package com.lumos.lumosspring.team.repository

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
                SELECT team_id from app_user
                where user_id = :userId 
            """.trimIndent(), params, Long::class.java)
        } catch (_: EmptyResultDataAccessException) {
            null
        }
    }
    fun renewTeam(teamId: Long, usersIds: List<UUID>) {
        val sql = """
        UPDATE app_user
        SET team_id = CASE
            WHEN user_id IN (:usersIds) THEN :teamId
        END
        WHERE team_id = :teamId
    """.trimIndent()

        val params = mapOf(
            "teamId" to teamId,
            "usersIds" to usersIds
        )

        jdbcTemplate.update(sql, params)
    }



}