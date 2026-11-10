package com.ipb.castelobranco.features.studies.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.ipb.castelobranco.core.data.local.TextScaleStore
import com.ipb.castelobranco.core.di.SettingsPrefs
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.BASE_FONT_SIZE
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DoctrinePreferences @Inject constructor(
    @param:SettingsPrefs dataStore: DataStore<Preferences>,
) : DoctrinePreferencesRepository {

    private val textScale = TextScaleStore(
        dataStore = dataStore,
        key = "doctrine_text_scale",
        legacySpKey = "doctrine_font_size",
        legacyBaseSp = BASE_FONT_SIZE,
    )

    override val textScaleFlow: Flow<Int> = textScale.flow

    override suspend fun setTextScale(percent: Int) = textScale.set(percent)
}
