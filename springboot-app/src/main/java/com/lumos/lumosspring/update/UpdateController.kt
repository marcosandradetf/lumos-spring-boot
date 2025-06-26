package com.lumos.lumosspring.update

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UpdateController {

    // Simulação dos dados da última versão (normalmente você buscaria do banco)
    private val latestVersionCode = 2L
    private val latestVersionName = "1.4.0"
    private val apkUrl = "https://minio.thryon.com.br/apk/com.thryon.lumos_v2.apk"

    @GetMapping("/mobile/check-update")
    fun checkUpdate(@RequestParam version: Long): Update {
        // Aqui você pode implementar lógica para decidir se tem update, etc.
        // Exemplo simples: sempre retorna a última versão
        return Update(
            latestVersionCode = latestVersionCode,
            latestVersionName = latestVersionName,
            apkUrl = apkUrl
        )
    }
}

data class Update(
    val latestVersionCode: Long,
    val latestVersionName: String,
    val apkUrl: String
)
