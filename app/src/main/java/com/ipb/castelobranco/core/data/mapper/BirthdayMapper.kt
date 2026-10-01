package com.ipb.castelobranco.core.data.mapper

import com.ipb.castelobranco.core.data.dto.BirthdaysResponseDto
import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.domain.model.Gender

fun BirthdaysResponseDto.toDomain(): List<Birthday> =
    birthdays
        .filter { it.name.isNotBlank() }
        .map {
            Birthday(name = it.name, month = it.birthMonth, day = it.birthDay, gender = it.gender.toGender())
        }
        .sortedWith(compareBy({ it.month }, { it.day }))

private fun String?.toGender(): Gender = when (this) {
    "M" -> Gender.MALE
    "F" -> Gender.FEMALE
    else -> Gender.UNKNOWN
}
