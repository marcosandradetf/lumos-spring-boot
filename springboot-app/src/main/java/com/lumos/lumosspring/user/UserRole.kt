package com.lumos.lumosspring.user

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable

//@Table("user_role")
//data class UserRole(
//    @Id val idUser: UUID,   // Chave primária: id do usuário
//    val idRole: Long,    // Chave primária: id do role
//
//    @Transient
//    private var isNewEntry: Boolean = true
//): Persistable<UUID> {
//    override fun getId(): UUID = idUser
//    override fun isNew(): Boolean = isNewEntry
//}
