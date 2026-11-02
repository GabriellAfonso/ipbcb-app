package com.ipb.castelobranco.core.network

import kotlinx.serialization.Serializable

@Serializable
data class UpdateChordChartRequest(
    val content: String,
    val tone: String,
    val instrument: String,
)
