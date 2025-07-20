package com.lumos.lumosspring.update

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UpdateController {

    private val latestVersionCode = 8L
    private val latestVersionName = "2.1.5"
    private val apkUrl = "https://minio.thryon.com.br/apk/com.thryon.apps.android.release_8_2.1.5.apk"

    @GetMapping("/mobile/check-update")
    fun checkUpdate(@RequestParam version: Long): ResponseEntity<Any> {
        return if (version < latestVersionCode) {
            ResponseEntity.ok(
                Update(
                    latestVersionCode = latestVersionCode,
                    latestVersionName = latestVersionName,
                    apkUrl = apkUrl
                )
            )
        } else {
            ResponseEntity.noContent().build() // 204 - Sem atualização
        }
    }

}

data class Update(
    val latestVersionCode: Long,
    val latestVersionName: String,
    val apkUrl: String
)
