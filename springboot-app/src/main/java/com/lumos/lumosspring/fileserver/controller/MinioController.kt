package com.lumos.lumosspring.fileserver.controller

import com.lumos.lumosspring.fileserver.service.MinioService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

@RestController
@RequestMapping("/minio")
class MinioController(private val minioService: MinioService) {

    @PostMapping("/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        val response = minioService.uploadFile(file, "scl-construtora")
        return ResponseEntity.ok(response)
    }

    @GetMapping("/download/{fileName}")
    fun downloadFile(@PathVariable fileName: String): ResponseEntity<InputStream> {
        val inputStream = minioService.downloadFile(fileName, "scl-construtora")
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(inputStream)
    }
}