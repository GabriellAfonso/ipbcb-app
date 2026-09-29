package com.ipb.castelobranco.features.gallery.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.ipb.castelobranco.core.di.SettingsPrefs
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GalleryPreferences @Inject constructor(
    @SettingsPrefs private val dataStore: DataStore<Preferences>,
) {
    private val layoutVersionKey = intPreferencesKey("gallery_layout_version")

    /** Disk layout the gallery files are in; `0` = the per-album folders of the first versions. */
    suspend fun layoutVersion(): Int = dataStore.data.first()[layoutVersionKey] ?: 0

    suspend fun setLayoutVersion(version: Int) {
        dataStore.edit { prefs -> prefs[layoutVersionKey] = version }
    }
}
