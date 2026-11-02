package com.ipb.castelobranco.features.profile.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.ipb.castelobranco.core.di.SettingsPrefs
import com.ipb.castelobranco.features.profile.domain.repository.RecoveryNoticeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecoveryNoticePreferences @Inject constructor(
    @param:SettingsPrefs private val dataStore: DataStore<Preferences>,
) : RecoveryNoticeRepository {

    private val answeredKey = booleanPreferencesKey("recovery_email_notice_answered")

    override val answered: Flow<Boolean> = dataStore.data.map { it[answeredKey] == true }

    override suspend fun markAnswered() {
        dataStore.edit { it[answeredKey] = true }
    }
}
