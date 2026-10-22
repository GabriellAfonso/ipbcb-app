package com.ipb.castelobranco.features.admin.schedule.data.mapper

import com.ipb.castelobranco.features.admin.schedule.data.dto.MemberItemDto
import com.ipb.castelobranco.features.admin.schedule.domain.model.Member

fun MemberItemDto.toDomain() = Member(id = id, name = name)
