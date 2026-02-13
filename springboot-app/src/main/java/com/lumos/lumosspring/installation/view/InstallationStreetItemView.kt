package com.lumos.lumosspring.installation.view

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table("installation_street_item_view")
data class InstallationStreetItemView(
    @Id
    val installationStreetItemId: Long,

    val installationStreetId: Long,
    val installationType: String,
    val contractItemId: Long,
    val executedQuantity: BigDecimal
)
