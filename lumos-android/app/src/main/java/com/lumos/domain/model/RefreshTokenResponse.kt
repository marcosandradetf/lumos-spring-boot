package com.lumos.domain.model

data class RefreshTokenResponse(val accessToken: String, val expiresIn: Long, val roles: String)
