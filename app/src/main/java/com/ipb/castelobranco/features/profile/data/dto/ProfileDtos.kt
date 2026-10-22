package com.ipb.castelobranco.features.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfilePhotoResponseDto(
    val detail: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null
)