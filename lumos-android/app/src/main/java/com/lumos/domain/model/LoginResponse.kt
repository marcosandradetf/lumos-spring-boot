package com.lumos.domain.model

data class LoginResponse(
    val accessToken: String,
    val expiresIn: Long,
    val roles: String,
    val teams: String,
    val refreshToken: String,
    val userUUID: String,
)