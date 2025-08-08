package com.lumos.api

import com.lumos.domain.model.LoginRequest
import com.lumos.domain.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @POST("api/mobile/auth/login")
    suspend fun login(
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/mobile/auth/refresh-token")
    suspend fun refreshToken(
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Authorization") refreshToken: String? // Token passado no header
    ): Response<LoginResponse>

    @POST("api/mobile/auth/logout")
    suspend fun logout(
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Authorization") refreshToken: String? // Token passado no header
    ): Response<Void>

}
