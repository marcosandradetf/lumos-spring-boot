package com.lumos.lumosspring.execution.entities

import com.lumos.lumosspring.util.ExecutionStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("direct_execution_street")
data class DirectExecutionStreet (
    @Id
    var directExecutionStreetId: Long = 0,
    var lastPower: String? = null,
    var streetName: String? = null,
    var number: Int? = null,
    var neighborhood: String? = null,
    var city: String? = null,
    var state: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var finishedAt: Instant? = null,
    var streetStatus: String = ExecutionStatus.FINISHED,
    var deviceStreetId: Long? = null,
    var deviceId: String? = null,
    var executionPhotoUri: String? = null,
)