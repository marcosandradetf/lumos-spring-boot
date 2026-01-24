package com.lumos.lumosspring.remoteconfig.dto


data class RemoteConfigResponse(
    val forceUpdate: Boolean,
    val updateType: String,
    val minBuild: Long,
    val features: Map<String, Boolean>,
    val actions: List<RemoteAction>
)

data class RemoteAction(
    val id: String,
    val type: String,
    val target: String,
    val minAppBuild: Long? = null,
    val conditions: Map<String, Any>? = null,
    val payload: Map<String, Any>? = null
)

data class RemoteConfigRequest(
    val appId: String,
    val platform: String,
    val minSupportedBuild: Long,
    val forceUpdate: Boolean = false,
    val updateType: String = "FLEXIBLE",
    val features: Map<String, Boolean>,
    val actions: List<RemoteActionRequest> = emptyList()
)

data class RemoteActionRequest(
    val actionType: String,
    val target: String,
    val minAppBuild: Long? = null,
    val conditions: Map<String, Any>? = null,
    val payload: Map<String, Any>? = null,
    val sortOrder: Int = 0,
    val active: Boolean = true
)
