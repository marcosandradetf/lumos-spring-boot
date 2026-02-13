package com.lumos.lumosspring.installation.view

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.math.BigDecimal

@Table("installation_street_view")
data class InstallationStreetView(

    @Id
    val installationStreetId: Long,

    val installationId: Long,
    val installationType: String,
    val address: String?,
    val lastPower: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val currentSupply: String?,
    val finishedAt: LocalDateTime?,
    val streetStatus: String,
    val deviceId: String,
    val executionPhotoUri: String?
)
