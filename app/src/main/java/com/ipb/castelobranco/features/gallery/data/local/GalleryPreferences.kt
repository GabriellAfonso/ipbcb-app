package com.ipb.castelobranco.features.gallery.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.ipb.castelobranco.core.di.SettingsPrefs
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryMediaSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GalleryPreferences @Inject constructor(
    @SettingsPrefs private val dataStore: DataStore<Preferences>,
) : GalleryMediaSettings {
    private val layoutVersionKey = intPreferencesKey("gallery_layout_version")
    private val syncPausedKey = booleanPreferencesKey("gallery_sync_paused")
    private val autoDownloadVideosKey = booleanPreferencesKey("gallery_auto_download_videos")
    private val videoCardDismissedKey = booleanPreferencesKey("gallery_video_card_dismissed")
    private val photoDownloadsPausedKey = booleanPreferencesKey("gallery_photo_downloads_paused")

    /** Disk layout the gallery files are in; `0` = the per-album folders of the first versions. */
    suspend fun layoutVersion(): Int = dataStore.data.first()[layoutVersionKey] ?: 0

    suspend fun setLayoutVersion(version: Int) {
        dataStore.edit { prefs -> prefs[layoutVersionKey] = version }
    }

    /** "Apagar galeria" paused every sync until the user taps "Baixar galeria". Survives sign-out. */
    suspend fun isSyncPaused(): Boolean = dataStore.data.first()[syncPausedKey] ?: false

    suspend fun setSyncPaused(paused: Boolean) {
        dataStore.edit { prefs -> prefs[syncPausedKey] = paused }
    }

    /** "Baixar vídeos automaticamente": off by default. A device setting — survives sign-out. */
    override val autoDownloadVideos: Flow<Boolean> = dataStore.data.map { it[autoDownloadVideosKey] ?: false }

    override suspend fun isAutoDownloadVideos(): Boolean = autoDownloadVideos.first()

    override suspend fun setAutoDownloadVideos(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[autoDownloadVideosKey] = enabled }
    }

    /** The "Há vídeos na galeria" card was answered: it never shows again on this device. */
    override val videoCardDismissed: Flow<Boolean> = dataStore.data.map { it[videoCardDismissedKey] ?: false }

    override suspend fun dismissVideoCard() {
        dataStore.edit { prefs -> prefs[videoCardDismissedKey] = true }
    }

    /** "Apagar galeria" with only Fotos: photos stop downloading until "Baixar galeria" with Fotos. */
    override val photoDownloadsPaused: Flow<Boolean> = dataStore.data.map { it[photoDownloadsPausedKey] ?: false }

    override suspend fun isPhotoDownloadsPaused(): Boolean = photoDownloadsPaused.first()

    override suspend fun setPhotoDownloadsPaused(paused: Boolean) {
        dataStore.edit { prefs -> prefs[photoDownloadsPausedKey] = paused }
    }
}
