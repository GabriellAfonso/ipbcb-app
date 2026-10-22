package com.ipb.castelobranco.features.auth.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshRequest(
    @SerialName("refresh") val refresh: String
)
