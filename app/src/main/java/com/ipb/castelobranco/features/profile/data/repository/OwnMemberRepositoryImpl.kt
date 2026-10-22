package com.ipb.castelobranco.features.profile.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.mapError
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.profile.data.api.ProfileApi
import com.ipb.castelobranco.features.profile.data.snapshot.toLocalDateOrNull
import com.ipb.castelobranco.features.profile.domain.model.OwnMember
import com.ipb.castelobranco.features.profile.domain.repository.OwnMemberRepository
import javax.inject.Inject

/** Straight from the server every time: the endpoint is `no-store` and the data never touches the disk. */
class OwnMemberRepositoryImpl @Inject constructor(
    private val api: ProfileApi,
) : OwnMemberRepository {

    override suspend fun get(): Result<OwnMember?> = runCatching {
        val response = api.getOwnMember()
        if (!response.isSuccessful) {
            val error = response.toAppError()
            if (error is AppError.Server && error.errorCode == MEMBER_NOT_LINKED) return@runCatching null
            throw error
        }
        val dto = response.body() ?: throw AppError.Unknown(message = EMPTY_BODY)
        OwnMember(
            role = dto.role?.takeIf { it.isNotBlank() },
            ministries = dto.ministries.filter { it.isNotBlank() },
            birthDay = dto.birthDay,
            birthMonth = dto.birthMonth,
            baptismDate = dto.baptismDate.toLocalDateOrNull(),
            gender = dto.gender,
        )
    }.mapError()

    private companion object {
        const val MEMBER_NOT_LINKED = "MEMBER_NOT_LINKED"
        const val EMPTY_BODY = "Resposta vazia"
    }
}
