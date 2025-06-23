package com.lumos.lumosspring.util

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

object JdbcUtil {

    inline fun <reified T> getDataNamed(
        namedJdbc: NamedParameterJdbcTemplate,
        sql: String,
        params: Map<String, Any>
    ): List<T> {
        return namedJdbc.query(
            sql,
            MapSqlParameterSource(params),
            BeanPropertyRowMapper(T::class.java)
        )
    }

    fun getRawData(
        namedJdbc: NamedParameterJdbcTemplate,
        sql: String,
        params: Map<String, Any?>
    ): List<Map<String, Any>> {
        return namedJdbc.queryForList(sql, MapSqlParameterSource(params))
    }

    fun getSingleRow(
        namedJdbc: NamedParameterJdbcTemplate,
        sql: String,
        params: Map<String, Any?>
    ): Map<String, Any>? {
        return try {
            namedJdbc.queryForMap(sql, MapSqlParameterSource(params))
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }


    fun existsRaw(
        namedJdbcTemplate: NamedParameterJdbcTemplate,
        sql: String,
        params: Map<String, Any?> = emptyMap()
    ): Boolean {
        return namedJdbcTemplate.queryForList(sql, params).isNotEmpty()
    }

    fun <T> getDescription(
        jdbcTemplate: JdbcTemplate,
        field: String,
        table: String,
        where: String? = null,
        equal: Any? = null,
        type: Class<T>,
        order: String? = null
    ): T? {
        var sql = "SELECT $field FROM $table"
        val args = mutableListOf<Any>()

        if (where != null && equal != null) {
            sql += " WHERE $where = ?"
            args.add(equal)
        }

        if (order != null) {
            sql += " ORDER BY $order" // cuidado: validar `order` se vier do usuário
        }

        sql += " LIMIT 1"

        return jdbcTemplate.query(sql, args.toTypedArray()) { rs, _ ->
            rs.getObject(1, type)
        }.firstOrNull()

    }

    fun <T> getDescriptions(
        jdbcTemplate: JdbcTemplate,
        field: String,
        table: String,
        where: String,
        type: Class<T>,
        order: String = "",
    ): List<T> {
        var sql = "SELECT $field FROM $table WHERE $where"
        if (order != "") sql += " ORDER BY $order"

        return jdbcTemplate.queryForList(sql, type)
    }

}
