package com.lumos.lumosspring.maintenance.repository

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class MaintenanceQueryRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {

    data class TypesMaterialDto(
        val description: String,
    )

    fun getMaterialsForMaintenance(): List<TypesMaterialDto> {
        return jdbcTemplate.query(
            """
                select distinct mt.type_name
                from material m 
                inner join material_type mt on mt.id_type = m.id_material_type
            """.trimIndent()
        ) { rs, _ ->
            TypesMaterialDto(
                rs.getString("type_name") ?: throw IllegalStateException("MaintenanceQueryRepository - TypeName is null"),
            )
        }
    }

}