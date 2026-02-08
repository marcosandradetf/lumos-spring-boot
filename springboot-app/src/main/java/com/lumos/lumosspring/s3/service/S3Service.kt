package com.lumos.lumosspring.s3.service

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ObjectIdentifier
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class S3Service(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner
) {

    fun uploadFile(
        file: MultipartFile,
        bucketName: String,
        folder: String,
        fileName: String,
        tenantId: UUID,
    ): String {
        val extension = file.originalFilename
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() }
            ?: ""

        val objectName =
            "tenants/$tenantId/$folder/${fileName}_file_${System.currentTimeMillis()}.$extension"

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .contentType(file.contentType)
                .build(),
            RequestBody.fromInputStream(file.inputStream, file.size)
        )

        return objectName
    }

    fun downloadFile(objectName: String, bucketName: String): InputStream {
        return s3Client.getObject(
            GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .build()
        )
    }

    fun getPresignedObjectUrl(
        bucketName: String,
        objectName: String,
        expiryAt: Int = 5 * 60
    ): String {
        val request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(objectName)
            .build()

        val presigned = s3Presigner.presignGetObject {
            it.signatureDuration(Duration.ofSeconds(expiryAt.toLong()))
            it.getObjectRequest(request)
        }

        return presigned.url().toString()
    }

    data class PublicUrlResponse(
        val url: String?,
        val expiresAt: Long?
    )

    fun getPublicUrl(
        bucketName: String,
        objectName: String?,
        expiryAt: Int = 5 * 60
    ): PublicUrlResponse {
        if (objectName == null) {
            return PublicUrlResponse(null, null)
        }

        val url = getPresignedObjectUrl(bucketName, objectName, expiryAt)
        val expiresAt = Instant.now().plusSeconds(expiryAt.toLong()).epochSecond

        return PublicUrlResponse(url, expiresAt)
    }

    fun deleteFiles(bucketName: String, objectNames: Set<String>) {
        val identifiers = objectNames.map {
            ObjectIdentifier.builder().key(it).build()
        }

        s3Client.deleteObjects(
            DeleteObjectsRequest.builder()
                .bucket(bucketName)
                .delete(
                    Delete.builder()
                        .objects(identifiers)
                        .build()
                )
                .build()
        )
    }
}
