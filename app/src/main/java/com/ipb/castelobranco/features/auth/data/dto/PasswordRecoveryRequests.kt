package com.ipb.castelobranco.features.auth.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PasswordRecoveryRequest(
    @SerialName("email") val email: String,
)

@Serializable
data class PasswordRecoveryConfirmRequest(
    @SerialName("email") val email: String,
    @SerialName("code") val code: String,
    @SerialName("new_password") val newPassword: String,
    @SerialName("new_password_confirm") val newPasswordConfirm: String,
)
