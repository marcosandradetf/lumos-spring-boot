package com.lumos.lumosspring.fileserver.controller

import com.lumos.lumosspring.fileserver.service.MinioService
import com.lumos.lumosspring.util.DefaultResponse
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
@RequestMapping("/api/minio")
class MinioController(private val minioService: MinioService) {

    @PostMapping("/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        val response = minioService.uploadFile(file, "scl-construtora","documents", "document")
        return ResponseEntity.ok(response)
    }

    @PostMapping("/upload-files")
    fun uploadFiles(@RequestParam("files") files: List<MultipartFile>): ResponseEntity<Any> {
        val responses = files.map { file ->
            minioService.uploadFile(file, "scl-construtora", "documents", "document")
        }


        return ResponseEntity.ok(responses)
    }


    @GetMapping("/download/{fileName}")
    fun downloadFile(@PathVariable fileName: String): ResponseEntity<InputStreamResource> {
        val inputStream: InputStream = minioService.downloadFile(fileName, "scl-construtora")
        val resource = InputStreamResource(inputStream)

        // Detecta o MIME type automaticamente
        val filePath = Paths.get(fileName)
        val contentType = Files.probeContentType(filePath) ?: MediaType.APPLICATION_OCTET_STREAM_VALUE

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource)
    }
}