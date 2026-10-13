package com.ipb.castelobranco.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.ipb.castelobranco.core.di.SettingsPrefs
import com.ipb.castelobranco.features.settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class ThemePreferences @Inject constructor(
    @param: SettingsPrefs private val dataStore: DataStore<Preferences>
) {

    companion object {
        private const val MODE_FOLLOW_SYSTEM: Int = 0
        private const val MODE_LIGHT: Int = 1
        private const val MODE_DARK: Int = 2
        private const val SCROLL_MODE_HORIZONTAL: Int = 0
        private const val SCROLL_MODE_VERTICAL: Int = 1
        const val DEFAULT_HYMNAL_FONT_SIZE: Float = 22f
    }

    private val themeModeKey = intPreferencesKey("theme_mode")
    private val hymnalFontSizeKey = floatPreferencesKey("hymnal_font_size")
    private val songScrollModeKey = intPreferencesKey("song_scroll_mode")
    private val birthdayNotificationsKey = booleanPreferencesKey("birthday_notifications_enabled")
    private val birthdayLastNotifiedKey = longPreferencesKey("birthday_last_notified_epoch_day")

    val themeModeFlow: Flow<ThemeMode> =
        dataStore.data.map { prefs ->
            when (prefs[themeModeKey] ?: MODE_FOLLOW_SYSTEM) {
                MODE_LIGHT -> ThemeMode.LIGHT
                MODE_DARK -> ThemeMode.DARK
                else -> ThemeMode.FOLLOW_SYSTEM
            }
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        val persisted = when (mode) {
            ThemeMode.FOLLOW_SYSTEM -> MODE_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> MODE_LIGHT
            ThemeMode.DARK -> MODE_DARK
        }
        dataStore.edit { prefs ->
            prefs[themeModeKey] = persisted
        }
    }

    suspend fun setFollowSystem() = setThemeMode(ThemeMode.FOLLOW_SYSTEM)
    suspend fun setLightMode() = setThemeMode(ThemeMode.LIGHT)
    suspend fun setDarkMode() = setThemeMode(ThemeMode.DARK)

    val hymnalFontSizeFlow: Flow<Float> =
        dataStore.data.map { prefs ->
            prefs[hymnalFontSizeKey] ?: DEFAULT_HYMNAL_FONT_SIZE
        }

    suspend fun setHymnalFontSize(size: Float) {
        dataStore.edit { prefs ->
            prefs[hymnalFontSizeKey] = size
        }
    }

    val songScrollModeFlow: Flow<SongScrollMode> =
        dataStore.data.map { prefs ->
            when (prefs[songScrollModeKey] ?: SCROLL_MODE_HORIZONTAL) {
                SCROLL_MODE_VERTICAL -> SongScrollMode.VERTICAL
                else -> SongScrollMode.HORIZONTAL
            }
        }

    suspend fun setSongScrollMode(mode: SongScrollMode) {
        val persisted = when (mode) {
            SongScrollMode.HORIZONTAL -> SCROLL_MODE_HORIZONTAL
            SongScrollMode.VERTICAL -> SCROLL_MODE_VERTICAL
        }
        dataStore.edit { prefs ->
            prefs[songScrollModeKey] = persisted
        }
    }

    val birthdayNotificationsFlow: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[birthdayNotificationsKey] ?: true
        }

    suspend fun setBirthdayNotifications(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[birthdayNotificationsKey] = enabled
        }
    }

    /** The day a birthday notification was last posted, so the 18:00 fallback skips a day already notified. */
    suspend fun birthdayLastNotified(): LocalDate? =
        dataStore.data.first()[birthdayLastNotifiedKey]?.let(LocalDate::ofEpochDay)

    suspend fun setBirthdayLastNotified(day: LocalDate) {
        dataStore.edit { prefs ->
            prefs[birthdayLastNotifiedKey] = day.toEpochDay()
        }
    }
}