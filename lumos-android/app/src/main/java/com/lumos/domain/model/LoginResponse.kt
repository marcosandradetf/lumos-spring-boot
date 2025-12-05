package com.lumos.domain.model

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
)