package com.ipb.castelobranco.features.studies.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.ipb.castelobranco.core.di.SettingsPrefs
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.DEFAULT_FONT_SIZE
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.MAX_FONT_SIZE
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.MIN_FONT_SIZE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DoctrinePreferences @Inject constructor(
    @param:SettingsPrefs private val dataStore: DataStore<Preferences>,
) : DoctrinePreferencesRepository {

    private val fontSizeKey = floatPreferencesKey("doctrine_font_size")

    override val fontSizeFlow: Flow<Float> =
        dataStore.data.map { prefs -> prefs[fontSizeKey] ?: DEFAULT_FONT_SIZE }

    override suspend fun setFontSize(size: Float) {
        dataStore.edit { prefs -> prefs[fontSizeKey] = size.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE) }
    }
}
