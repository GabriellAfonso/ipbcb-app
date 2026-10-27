package com.ipb.castelobranco.features.settings.data.repository

import com.ipb.castelobranco.core.data.local.ThemePreferences
import com.ipb.castelobranco.features.settings.domain.model.ThemeMode
import com.ipb.castelobranco.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val themePreferences: ThemePreferences
) : SettingsRepository {

    override val themeModeFlow: Flow<ThemeMode> = themePreferences.themeModeFlow

    override suspend fun setThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    override suspend fun setFollowSystem() {
        themePreferences.setFollowSystem()
    }

    override suspend fun setLightMode() {
        themePreferences.setLightMode()
    }

    override suspend fun setDarkMode() {
        themePreferences.setDarkMode()
    }

    override val hymnalFontSizeFlow: Flow<Float> = themePreferences.hymnalFontSizeFlow

    override suspend fun setHymnalFontSize(size: Float) {
        themePreferences.setHymnalFontSize(size)
    }

    override val birthdayNotificationsFlow: Flow<Boolean> = themePreferences.birthdayNotificationsFlow

    override suspend fun setBirthdayNotifications(enabled: Boolean) {
        themePreferences.setBirthdayNotifications(enabled)
    }

    override val scheduleNotificationsFlow: Flow<Boolean> = themePreferences.scheduleNotificationsFlow

    override suspend fun setScheduleNotifications(enabled: Boolean) {
        themePreferences.setScheduleNotifications(enabled)
    }

    override val keepScreenOnFlow: Flow<Boolean> = themePreferences.keepScreenOnFlow

    override suspend fun setKeepScreenOn(enabled: Boolean) {
        themePreferences.setKeepScreenOn(enabled)
    }
}