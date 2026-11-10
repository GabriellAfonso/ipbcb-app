package com.ipb.castelobranco.features.settings.domain.repository

import com.ipb.castelobranco.features.settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val themeModeFlow: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setFollowSystem()
    suspend fun setLightMode()
    suspend fun setDarkMode()

    /** The hymn text size, in percent (`specs/core/spec.md` §6.7.1). */
    val hymnalTextScaleFlow: Flow<Int>

    suspend fun setHymnalTextScale(percent: Int)

    val birthdayNotificationsFlow: Flow<Boolean>

    suspend fun setBirthdayNotifications(enabled: Boolean)

    val scheduleNotificationsFlow: Flow<Boolean>

    suspend fun setScheduleNotifications(enabled: Boolean)

    val keepScreenOnFlow: Flow<Boolean>

    suspend fun setKeepScreenOn(enabled: Boolean)
}