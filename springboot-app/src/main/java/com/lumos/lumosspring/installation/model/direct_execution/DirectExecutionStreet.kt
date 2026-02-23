package com.lumos.lumosspring.installation.model.direct_execution

import com.lumos.lumosspring.util.ExecutionStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("direct_execution_street")
data class DirectExecutionStreet (
    @Id
    var directExecutionStreetId: Long? = null,
    var lastPower: String? = null,
    var address: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var finishedAt: Instant? = null,
    var streetStatus: String = ExecutionStatus.FINISHED,
    var deviceStreetId: Long? = null,
    var deviceId: String? = null,
    var executionPhotoUri: String? = null,
    var directExecutionId: Long,
    val currentSupply: String?,
    val pointNumber: Int?
)