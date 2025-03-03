package com.lumos.domain.model

import java.util.UUID

data class LoginResponse(
    val accessToken: String,
    val expiresIn: Long,
    val roles: String,
    val refreshToken: String,
    val userUUID: String,
)