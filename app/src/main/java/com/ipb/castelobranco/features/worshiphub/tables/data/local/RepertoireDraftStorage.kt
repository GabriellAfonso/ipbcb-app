package com.ipb.castelobranco.features.worshiphub.tables.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ipb.castelobranco.core.di.SetlistPrefs
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.DraftRowDto
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.RepertoireDraftDto
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.DraftRow
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.RepertoireDraft
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.RepertoireDraftRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Shares the DataStore of the manual pins: both are device-local song choices for the Sunday. */
@Singleton
class RepertoireDraftStorage @Inject constructor(
    @param:SetlistPrefs private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : RepertoireDraftRepository {

    private object Keys {
        val ROWS = stringPreferencesKey("repertoire_draft_v1")
        val UPDATED_AT = longPreferencesKey("repertoire_draft_updated_at")
    }

    override suspend fun load(): RepertoireDraft? {
        val prefs = dataStore.data.first()
        val raw = prefs[Keys.ROWS] ?: return null
        val updatedAt = prefs[Keys.UPDATED_AT] ?: return null
        val dto = runCatching { json.decodeFromString(RepertoireDraftDto.serializer(), raw) }
            .onFailure { Timber.w(it, "Repertoire draft unreadable; discarding") }
            .getOrNull()
        if (dto == null) {
            clear()
            return null
        }
        return RepertoireDraft(
            rows = dto.rows.map { DraftRow(it.position, it.songId, it.tone, it.isFixed) },
            updatedAtMillis = updatedAt,
        )
    }

    override suspend fun save(draft: RepertoireDraft) {
        val dto = RepertoireDraftDto(draft.rows.map { DraftRowDto(it.position, it.songId, it.tone, it.isFixed) })
        dataStore.edit { prefs ->
            prefs[Keys.ROWS] = json.encodeToString(RepertoireDraftDto.serializer(), dto)
            prefs[Keys.UPDATED_AT] = draft.updatedAtMillis
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.ROWS)
            prefs.remove(Keys.UPDATED_AT)
        }
    }
}
