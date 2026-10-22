package com.ipb.castelobranco.features.worshiphub.register.data.mapper

import com.ipb.castelobranco.features.worshiphub.register.data.dto.RegisterSundayPlayItemDto
import com.ipb.castelobranco.features.worshiphub.register.data.dto.RegisterSundayPlaysRequestDto
import com.ipb.castelobranco.features.worshiphub.register.domain.model.SundayPlayPushItem

fun buildRegisterRequest(date: String, plays: List<SundayPlayPushItem>): RegisterSundayPlaysRequestDto =
    RegisterSundayPlaysRequestDto(
        date = date,
        plays = plays.map {
            RegisterSundayPlayItemDto(
                songId = it.songId,
                position = it.position,
                tone = it.tone
            )
        }
    )
