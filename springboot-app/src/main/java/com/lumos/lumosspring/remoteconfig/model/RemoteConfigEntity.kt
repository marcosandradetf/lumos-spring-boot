package com.lumos.lumosspring.remoteconfig.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("remote_config")
data class RemoteConfigEntity(
    @Id
    @Column("remote_config_id")
    val remoteConfigId: Long? = null,

    val appId: String,
    val platform: String,
    val minSupportedBuild: Long,
    val forceUpdate: Boolean = false,
    val updateType: String = "FLEXIBLE",
    val featuresJson: String = "{}",
    val active: Boolean = true,
    val updatedAt: OffsetDateTime? = null,

    @MappedCollection(idColumn = "config_id")
    val actions: Set<RemoteConfigActionEntity> = emptySet()
)

