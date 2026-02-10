package com.lumos.lumosspring.s3.controller

import com.lumos.lumosspring.s3.service.S3Service
import com.lumos.lumosspring.util.Utils
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths

@RestController
@RequestMapping("/api")
class S3Controller(private val service: S3Service) {

    @PostMapping("/s3/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        val response = service.uploadFile(
            file,
            Utils.getCurrentBucket(),
            "documents",
            "document",
            Utils.getCurrentTenantId()
        )
        return ResponseEntity.ok(response)
    }


    @GetMapping("/s3/download/{fileName}")
    fun downloadFile(@PathVariable fileName: String): ResponseEntity<InputStreamResource> {
        val inputStream: InputStream = service.downloadFile(fileName, Utils.getCurrentBucket())
        val resource = InputStreamResource(inputStream)

        // Detecta o MIME type automaticamente
        val filePath = Paths.get(fileName)
        val contentType = Files.probeContentType(filePath) ?: MediaType.APPLICATION_OCTET_STREAM_VALUE

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource)
    }

    @GetMapping("/mobile/s3/download/{fileName}")
    fun downloadFileMobile(@PathVariable fileName: String): ResponseEntity<InputStreamResource> = downloadFile(fileName)
}