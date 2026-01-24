package com.lumos.lumosspring.remoteconfig

import com.fasterxml.jackson.databind.ObjectMapper
import com.lumos.lumosspring.remoteconfig.dto.RemoteAction
import com.lumos.lumosspring.remoteconfig.dto.RemoteConfigRequest
import com.lumos.lumosspring.remoteconfig.dto.RemoteConfigResponse
import com.lumos.lumosspring.remoteconfig.model.*
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

@Service
class RemoteConfigService(
    private val repo: RemoteConfigRepository,
    private val objectMapper: ObjectMapper
) {
    private val allowedApps = setOf("lumos-android", "lumos-ios")
    private val allowedPlatforms = setOf("android", "ios")

    @Cacheable(
        value = ["getRemoteConfigs"],
        key = "#appId + ':' + #platform"
    )
    fun getConfig(appId: String, platform: String, appBuild: Long): RemoteConfigResponse {
        validateHeaders(appId, platform, appBuild)

        val entity = repo.findByAppIdAndPlatformAndActiveTrue(appId, platform)
            ?: return RemoteConfigResponse(
                forceUpdate = false,
                updateType = "FLEXIBLE",
                minBuild = 0,
                features = emptyMap(),
                actions = emptyList(),
            )

        val features: Map<String, Boolean> =
            objectMapper.readValue(
                entity.featuresJson,
                objectMapper.typeFactory.constructMapType(Map::class.java, String::class.java, Boolean::class.java)
            )

        val force = entity.forceUpdate || appBuild < entity.minSupportedBuild
        val updateType = if (force) "IMMEDIATE" else entity.updateType

        val actions = entity.actions
            .asSequence()
            .filter { it.active }
            .filter { it.minAppBuild == null || appBuild >= it.minAppBuild }
            .sortedBy { it.sortOrder }
            .map { a ->
                RemoteAction(
                    id = a.actionKey,
                    type = a.actionType,
                    target = a.target,
                    minAppBuild = a.minAppBuild,
                    conditions = a.conditionsJson?.let {
                        objectMapper.readValue(
                            it,
                            objectMapper.typeFactory.constructMapType(
                                Map::class.java,
                                String::class.java,
                                Any::class.java
                            )
                        )
                    },
                    payload = a.payloadJson?.let { objectMapper.readValue(it, Map::class.java) as Map<String, Any> }
                )
            }
            .toList()

        return RemoteConfigResponse(
            forceUpdate = force,
            updateType = updateType,
            minBuild = entity.minSupportedBuild,
            features = features,
            actions = actions
        )
    }

    private fun validateHeaders(appId: String, platform: String, build: Long) {
        if (appId !in allowedApps) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid app id")
        if (platform.lowercase() !in allowedPlatforms) throw ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Invalid platform"
        )
        if (build <= 0) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid build")
    }

    @Caching(
        evict = [CacheEvict(
            cacheNames = ["getRemoteConfigs"],
            allEntries = true
        )]
    )
    fun setConfig(
        body: RemoteConfigRequest
    ) {
        // normalização mínima
        val platform = body.platform.lowercase()

        // desativa config anterior (se existir)
        repo.findByAppIdAndPlatformAndActiveTrue(body.appId, platform)?.let { current ->
            repo.save(
                current.copy(
                    active = false,
                    updatedAt = OffsetDateTime.now()
                )
            )
        }

        // cria nova config
        val entity = RemoteConfigEntity(
            appId = body.appId,
            platform = platform,
            minSupportedBuild = body.minSupportedBuild,
            forceUpdate = body.forceUpdate,
            updateType = body.updateType,
            featuresJson = objectMapper.writeValueAsString(body.features),
            active = true,
            updatedAt = OffsetDateTime.now(),
            actions = body.actions.map { a ->
                RemoteConfigActionEntity(
                    actionKey = UUID.randomUUID().toString(),
                    actionType = a.actionType,
                    target = a.target,
                    minAppBuild = a.minAppBuild,
                    conditionsJson = a.conditions?.let { objectMapper.writeValueAsString(it) },
                    payloadJson = a.payload?.let { objectMapper.writeValueAsString(it) },
                    sortOrder = a.sortOrder,
                    active = a.active
                )
            }.toSet()
        )

        repo.save(entity)
    }
}
