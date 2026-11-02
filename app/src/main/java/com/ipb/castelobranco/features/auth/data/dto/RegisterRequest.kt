package com.ipb.castelobranco.features.auth.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    @SerialName("recovery_email")
    val recoveryEmail: String,
    val password: String,
    @SerialName("password_confirm")
    val passwordConfirm: String
)
