package com.ipb.castelobranco.core.data.push

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ipb.castelobranco.core.di.PushPrefs
import com.ipb.castelobranco.core.domain.push.RegisteredTokenStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushTokenStore @Inject constructor(
    @param:PushPrefs private val dataStore: DataStore<Preferences>,
) : RegisteredTokenStore {

    private val key = stringPreferencesKey("registered_push_token")

    override suspend fun get(): String? = dataStore.data.first()[key]

    override suspend fun set(token: String) {
        dataStore.edit { it[key] = token }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }
}
