package com.ipb.castelobranco.features.auth.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ipb.castelobranco.core.data.security.AuthPrefsFiles
import com.ipb.castelobranco.core.di.AuthPrefs
import com.ipb.castelobranco.features.auth.domain.model.AuthTokens
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json

@Singleton
class TokenStorage @Inject constructor(
    @param: AuthPrefs private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    private object Keys {
        val TOKENS = stringPreferencesKey(AuthPrefsFiles.AUTH_TOKENS_KEY)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val tokensFlow: Flow<AuthTokens?> =
        dataStore.data.map { prefs ->
            val raw = prefs[Keys.TOKENS] ?: return@map null
            runCatching { json.decodeFromString(AuthTokens.serializer(), raw) }.getOrNull()
        }

    val tokensState: StateFlow<AuthTokens?> =
        tokensFlow.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    fun peekOrNull(): AuthTokens? = tokensState.value

    suspend fun save(tokens: AuthTokens) {
        dataStore.edit { prefs ->
            prefs[Keys.TOKENS] = json.encodeToString(AuthTokens.serializer(), tokens)
        }
    }

    suspend fun loadOrNull(): AuthTokens? = tokensState.value

    suspend fun clear() {
        dataStore.edit { it.remove(Keys.TOKENS) }
    }
}