package com.ipb.castelobranco.features.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    @SerialName("name") val name: String,
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
    @SerialName("new_password_confirm") val newPasswordConfirm: String,
)

/** The pair that replaces the session after a password change; the old one is already dead on the server. */
@Serializable
data class SessionTokensDto(
    @SerialName("access") val access: String,
    @SerialName("refresh") val refresh: String,
)

@Serializable
data class DeleteAccountWithPasswordRequest(
    @SerialName("password") val password: String,
)

@Serializable
data class DeleteAccountWithGoogleRequest(
    @SerialName("id_token") val idToken: String,
)
