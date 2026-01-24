package com.lumos.lumosspring.remoteconfig.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("remote_config_action")
data class RemoteConfigActionEntity(
    @Id
    @Column("remote_config_action_id")
    val remoteConfigActionId: Long? = null,
//    @Column("config_id")
//    val configId: Long? = null,
    val actionKey: String,
    val actionType: String,
    val target: String,
    val minAppBuild: Long? = null,
    val conditionsJson: String? = null,
    val payloadJson: String? = null,
    val sortOrder: Int = 0,
    val active: Boolean = true
)