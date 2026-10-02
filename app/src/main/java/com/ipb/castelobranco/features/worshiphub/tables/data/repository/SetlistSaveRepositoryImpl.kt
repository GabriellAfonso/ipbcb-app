package com.ipb.castelobranco.features.worshiphub.tables.data.repository

import com.ipb.castelobranco.core.data.setlist.toDomain
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.worshiphub.tables.data.api.SaveSetlistBody
import com.ipb.castelobranco.features.worshiphub.tables.data.api.SaveSetlistItemBody
import com.ipb.castelobranco.features.worshiphub.tables.data.api.SetlistSaveApi
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SaveSetlistFailure
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SaveSetlistResult
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SetlistEntry
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SetlistSaveRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetlistSaveRepositoryImpl @Inject constructor(
    private val api: SetlistSaveApi,
    private val json: Json,
) : SetlistSaveRepository {

    override suspend fun save(date: LocalDate, entries: List<SetlistEntry>): SaveSetlistResult {
        val body = SaveSetlistBody(entries.map { SaveSetlistItemBody(it.songId, it.position, it.tone) })
        return try {
            val response = api.save(date.toString(), body)
            if (!response.isSuccessful) {
                SaveSetlistResult.Failed(response.toAppError().toFailure())
            } else {
                val dto = response.body() ?: throw AppError.Server(code = response.code(), message = "Resposta vazia")
                SaveSetlistResult.Saved(dto.toDomain())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            SaveSetlistResult.Failed(e.toAppError().toFailure())
        }
    }

    private fun AppError.toFailure(): SaveSetlistFailure = when {
        this is AppError.Auth && code == HTTP_FORBIDDEN -> SaveSetlistFailure.NoPermission
        this is AppError.Server && code == HTTP_BAD_REQUEST -> SaveSetlistFailure.Invalid(this)
        this is AppError.Server && code == HTTP_NOT_FOUND -> SaveSetlistFailure.MissingSongs(missingSongCount())
        this is AppError.Network -> SaveSetlistFailure.NoConnection
        else -> SaveSetlistFailure.Other(this)
    }

    /** `missing_song_ids` arrives as JSON text in the extras that `ResponseExt` kept; at least one is missing. */
    private fun AppError.Server.missingSongCount(): Int {
        val raw = extras?.get(MISSING_SONG_IDS) ?: return 1
        val ids = runCatching { json.parseToJsonElement(raw).jsonArray }.getOrNull() ?: JsonArray(emptyList())
        return ids.size.coerceAtLeast(1)
    }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val MISSING_SONG_IDS = "missing_song_ids"
    }
}
