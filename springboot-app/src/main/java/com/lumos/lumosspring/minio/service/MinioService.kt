package com.lumos.lumosspring.minio.service

import io.minio.*
import io.minio.errors.MinioException
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import io.minio.http.Method


@Service
class MinioService(private val minioClient: MinioClient) {

    fun uploadFile(file: MultipartFile, bucketName: String, folder: String, type: String): String {
        try {
            // Verifica se o bucket existe, se não, cria
            val found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
            }

            val extension = file.originalFilename?.substringAfterLast('.', "") ?: ""
            val objectName = "$folder/${type}_file_${System.currentTimeMillis()}.$extension"

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .stream(file.inputStream, file.size, -1)
                    .contentType(file.contentType)
                    .build()
            )

            return objectName

        } catch (e: MinioException) {
            throw RuntimeException("Erro ao fazer upload para o MinIO: ${e.message}", e)
        }
    }

    fun downloadFile(objectName: String, bucketName: String): InputStream {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .build()
        )
    }

    fun getPresignedObjectUrl(bucketName: String, objectName: String): String {
        val url = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .`object`(objectName)
                .expiry(5 * 60) // em segundos (5 minutos)
                .build()
        )
        return url
    }

}