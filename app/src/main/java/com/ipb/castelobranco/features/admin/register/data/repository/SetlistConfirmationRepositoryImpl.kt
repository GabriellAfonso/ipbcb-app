package com.ipb.castelobranco.features.admin.register.data.repository

import com.ipb.castelobranco.core.data.setlist.toDomain
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.mapError
import com.ipb.castelobranco.core.domain.setlist.SundaySetlist
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.admin.register.data.api.SetlistAdminApi
import com.ipb.castelobranco.features.admin.register.domain.repository.SetlistConfirmationRepository
import retrofit2.Response
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetlistConfirmationRepositoryImpl @Inject constructor(
    private val api: SetlistAdminApi,
) : SetlistConfirmationRepository {

    override suspend fun byDate(date: LocalDate): Result<SundaySetlist> = runCatching {
        api.byDate(date.toString()).bodyOrThrow().toDomain()
    }.mapError()

    override suspend fun pending(): Result<List<SundaySetlist>> = runCatching {
        api.pending().bodyOrThrow().map { it.toDomain() }
    }.mapError()

    override suspend fun delete(date: LocalDate): Result<Unit> = runCatching {
        val response = api.delete(date.toString())
        if (!response.isSuccessful) throw response.toAppError()
    }.mapError()

    private fun <T> Response<T>.bodyOrThrow(): T {
        if (!isSuccessful) throw toAppError()
        return body() ?: throw AppError.Server(code = code(), message = "Resposta vazia")
    }
}
