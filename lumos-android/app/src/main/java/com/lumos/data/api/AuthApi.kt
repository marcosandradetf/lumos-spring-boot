package com.lumos.data.api

import com.lumos.domain.model.LoginRequest
import com.lumos.domain.model.LoginResponse
import com.lumos.domain.model.RefreshTokenRequest
import com.lumos.domain.model.RefreshTokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

import retrofit2.http.Header

interface AuthApi {
    @POST("/api/mobile/auth/login")
    fun login(
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: LoginRequest
    ): Call<LoginResponse>

    @POST("/api/mobile/auth/refresh-token")
    fun refreshToken(
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Authorization") refreshToken: String // Token passado no header
    ): Call<LoginResponse>

    @POST("/api/mobile/auth/logout")
    fun logout(
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Authorization") refreshToken: String // Token passado no header
    ): Call<Void>
}
