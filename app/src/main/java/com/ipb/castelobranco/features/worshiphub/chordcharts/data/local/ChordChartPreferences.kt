package com.ipb.castelobranco.features.worshiphub.chordcharts.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.ipb.castelobranco.core.data.local.TextScaleStore
import com.ipb.castelobranco.core.di.SettingsPrefs
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * The chord chart screen's text size on this phone, one for every chart (`specs/worshiphub/spec.md` §4.3). It was
 * always in percent; an old 80–160% in steps of 10 is read normalized (`specs/core/spec.md` §6.7.1).
 */
class ChordChartPreferences @Inject constructor(
    @param:SettingsPrefs dataStore: DataStore<Preferences>,
) {
    private val textScale = TextScaleStore(dataStore = dataStore, key = "chord_text_scale")

    /** Percent of the normal size. */
    val textScaleFlow: Flow<Int> = textScale.flow

    suspend fun setTextScale(percent: Int) = textScale.set(percent)
}
