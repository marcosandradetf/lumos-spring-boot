package com.lumos.domain.model

data class ActivateFirstAccessRequest(
    val cpf: String,
    val activationCode: String,
    val newPassword: String
)
