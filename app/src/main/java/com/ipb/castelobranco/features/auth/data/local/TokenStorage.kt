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
            prefs[Keys.TOKENS]?.let(::decodeOrNull)
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

    /**
     * Atomic compare-and-set on the stored pair: writes [replacement] — or removes the pair when it is `null` —
     * only if the stored refresh token is still [expectedRefresh]. Returns the pair stored after the edit.
     *
     * A token renewal started from a pair that was replaced meanwhile (a password change hands the app a new pair and
     * revokes every old refresh token) must neither clear nor overwrite the newer pair.
     */
    suspend fun swapIfRefresh(expectedRefresh: String, replacement: AuthTokens?): AuthTokens? {
        val prefs = dataStore.edit { prefs ->
            val current = prefs[Keys.TOKENS]?.let(::decodeOrNull)
            if (current?.refresh == expectedRefresh) {
                if (replacement == null) {
                    prefs.remove(Keys.TOKENS)
                } else {
                    prefs[Keys.TOKENS] = json.encodeToString(AuthTokens.serializer(), replacement)
                }
            }
        }
        return prefs[Keys.TOKENS]?.let(::decodeOrNull)
    }

    private fun decodeOrNull(raw: String): AuthTokens? =
        runCatching { json.decodeFromString(AuthTokens.serializer(), raw) }.getOrNull()

    suspend fun clear() {
        dataStore.edit { it.remove(Keys.TOKENS) }
    }
}