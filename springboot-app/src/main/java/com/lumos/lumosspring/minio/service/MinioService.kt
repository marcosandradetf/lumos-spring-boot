package com.lumos.lumosspring.minio.service

import com.lumos.lumosspring.util.Utils
import io.minio.*
import io.minio.errors.MinioException
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import io.minio.http.Method
import io.minio.messages.DeleteObject


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

    fun getPresignedObjectUrl(bucketName: String, objectName: String, expiryAt: Long = 5 * 60): String {
        val url = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .`object`(objectName)
                .expiry(expiryAt) // em segundos (5 minutos)
                .build()
        )
        return url
    }

    fun deleteFiles(bucketName: String, objectNames: Set<String>) {
        val objects = objectNames.map { DeleteObject(it) }

        val results = minioClient.removeObjects(
            RemoveObjectsArgs.builder()
                .bucket(bucketName)
                .objects(objects)
                .build()
        )

        // Itera sobre possíveis erros
        results.forEach { result ->
            try {
                result.get() // dispara exceção se houve erro ao remover esse objeto
            } catch (e: Exception) {
                throw Utils.BusinessException("Erro ao fazer delete para o MinIO: ${e.message}")
            }
        }
    }


}