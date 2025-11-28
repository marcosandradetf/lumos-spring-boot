package com.lumos.lumosspring.config

import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
open class MinioConfig {
    @Value("\${minio.public.url}")
    private lateinit var minioPublicUrl: String

    @Value("\${minio.url}")
    private lateinit var minioUrl: String

    @Value("\${minio.access-key}")
    private lateinit var accessKey: String

    @Value("\${minio.secret-key}")
    private lateinit var secretKey: String

    @Bean("internalMinioClient")
    open fun minioClient(): MinioClient {
        return MinioClient.builder()
            .endpoint(minioUrl)
            .credentials(accessKey, secretKey)
            .build()
    }

    @Bean("publicMinioClient")
    fun publicClient(): MinioClient {
        return MinioClient.builder()
            .endpoint(minioPublicUrl)
            .credentials(accessKey, secretKey)
            .build()
    }

}
