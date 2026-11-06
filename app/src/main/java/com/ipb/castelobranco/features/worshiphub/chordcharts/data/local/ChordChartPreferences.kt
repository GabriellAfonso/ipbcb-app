package com.ipb.castelobranco.features.worshiphub.chordcharts.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.ipb.castelobranco.core.di.SettingsPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** The chord chart screen's text size on this phone, one for every chart (`specs/worshiphub/spec.md` §4.3). */
class ChordChartPreferences @Inject constructor(
    @param:SettingsPrefs private val dataStore: DataStore<Preferences>,
) {
    /** Percent of the normal size. */
    val textScaleFlow: Flow<Int> = dataStore.data.map { it[TEXT_SCALE] ?: DEFAULT_TEXT_SCALE }

    suspend fun setTextScale(percent: Int) {
        dataStore.edit { it[TEXT_SCALE] = percent }
    }

    companion object {
        const val DEFAULT_TEXT_SCALE = 100
        private val TEXT_SCALE = intPreferencesKey("chord_text_scale")
    }
}
