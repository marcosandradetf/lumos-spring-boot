package com.lumos.lumosspring.fileserver.service

import io.minio.*
import io.minio.errors.MinioException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

@Service
class MinioService(private val minioClient: MinioClient) {

    fun uploadFile(file: MultipartFile, bucketName: String): String {
        try {
            // Verifica se o bucket existe, se não, cria
            val found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
            }

            val objectName = file.originalFilename ?: "file-${System.currentTimeMillis()}"

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
}