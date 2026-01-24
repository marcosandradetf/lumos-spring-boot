package com.lumos.lumosspring.remoteconfig

import com.lumos.lumosspring.remoteconfig.dto.RemoteConfigRequest
import com.lumos.lumosspring.remoteconfig.dto.RemoteConfigResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/remote-config")
class RemoteConfigController(
    private val service: RemoteConfigService
) {
    @GetMapping
    fun ping(): ResponseEntity<Void> {
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/get-config")
    fun getConfig(
        @RequestHeader("X-App-Id") appId: String,
        @RequestHeader("X-Platform") platform: String,
        @RequestHeader("X-App-Build") appBuild: Long
    ): ResponseEntity<*> {
        return ResponseEntity.ok(service.getConfig(appId, platform, appBuild))
    }

    @PreAuthorize("hasAuthority('SCOPE_SUPPORT')")
    @PostMapping("/set-config")
    fun setConfig(
        @RequestBody body: RemoteConfigRequest
    ): ResponseEntity<RemoteConfigResponse> {
        service.setConfig(body)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/post-payload")
    fun postPayload(
        @RequestBody body: List<Any>,
        @RequestParam("type") type: String,
    ): ResponseEntity<RemoteConfigResponse> {
        return ResponseEntity.noContent().build()
    }
}
