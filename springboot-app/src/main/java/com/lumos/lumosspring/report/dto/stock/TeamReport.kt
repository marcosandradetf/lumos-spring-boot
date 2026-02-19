package com.lumos.lumosspring.report.dto.stock

import com.fasterxml.jackson.annotation.JsonProperty

data class TeamReport(
    @field:JsonProperty("team_name")
    val teamName: String,
    @field:JsonProperty("company_logo")
    val companyLogo: String?,
    @field:JsonProperty("fantasy_name")
    val fantasyName: String,
    @field:JsonProperty("company_address")
    val companyAddress: String,
    @field:JsonProperty("company_cnpj")
    val companyCnpj: String,
    @field:JsonProperty("company_phone")
    val companyPhone: String,
    val installations: List<InstallationReport>
)

data class InstallationReport(
    @field:JsonProperty("installation_id")
    val installationId: Long,
    val description: String,
    @field:JsonProperty("installation_type")
    val installationType: String,
    val status: String,
    @field:JsonProperty("contract_id")
    val contractId: Long,
    val records: List<MaterialRecord>
)

data class MaterialRecord(
    @field:JsonProperty("material_name")
    val materialName: String,
    @field:JsonProperty("released_quantity")
    val releasedQuantity: Int,
    @field:JsonProperty("quantity_completed")
    val quantityCompleted: Int,
    val balance: Int,
    @field:JsonProperty("truck_quantity")
    val truckQuantity: Int,
    @field:JsonProperty("deposit_name")
    val depositName: String,
    @field:JsonProperty("created_at")
    val createdAt: String,
    val creator: String,
    @field:JsonProperty("collected_at")
    val collectedAt: String?,
    val responsible: String?
)
