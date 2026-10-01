package com.ipb.castelobranco.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BirthdaysResponseDto(
    val birthdays: List<BirthdayDto>
)

@Serializable
data class BirthdayDto(
    val name: String,
    val gender: String? = null,
    @SerialName("birth_month")
    val birthMonth: Int,
    @SerialName("birth_day")
    val birthDay: Int,
)
