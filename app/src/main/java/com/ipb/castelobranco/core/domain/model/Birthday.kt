package com.ipb.castelobranco.core.domain.model

data class Birthday(
    val name: String,
    val month: Int,
    val day: Int,
    val gender: Gender = Gender.UNKNOWN,
)

enum class Gender { MALE, FEMALE, UNKNOWN }
