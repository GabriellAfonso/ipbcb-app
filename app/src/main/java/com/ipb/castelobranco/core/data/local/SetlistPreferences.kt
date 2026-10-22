package com.ipb.castelobranco.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ipb.castelobranco.core.di.SetlistPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class SetlistPreferences @Inject constructor(
    @param:SetlistPrefs private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val DATE     = stringPreferencesKey("setlist_date")
        val SONG_IDS = stringPreferencesKey("setlist_song_ids_v1")
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val pinnedSongIds: Flow<List<Int>> = dataStore.data.map { prefs ->
        if (prefs[Keys.DATE] != today()) emptyList()
        else prefs[Keys.SONG_IDS].decodeIds()
    }

    suspend fun toggleSong(songId: Int) {
        dataStore.edit { prefs ->
            ensureToday(prefs)
            val updated = prefs[Keys.SONG_IDS].decodeIds().toggle(songId)
            prefs[Keys.SONG_IDS] = updated.encodeIds()
        }
    }

    private fun ensureToday(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        val today = today()
        if (prefs[Keys.DATE] != today) {
            prefs[Keys.DATE]     = today
            prefs[Keys.SONG_IDS] = ""
        }
    }

    private fun String?.decodeIds(): List<Int> =
        if (isNullOrBlank()) emptyList()
        else split(",").mapNotNull { it.toIntOrNull() }

    private fun List<Int>.encodeIds(): String = joinToString(",")

    private fun List<Int>.toggle(id: Int): List<Int> =
        if (id in this) this - id else this + id
}
