package com.lumos.lumosspring.user

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("user_role")
data class UserRole(
    @Id val idUser: UUID,   // Chave primária: id do usuário
    val idRole: Long    // Chave primária: id do role
)


data class UserRoleId(
    val idUser: UUID,
    val idRole: Long
)

