package com.ipb.castelobranco.features.profile.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ipb.castelobranco.core.di.AuthPrefs
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Where the recovery address lives on the device: never in the plain profile snapshot. [load] returns `null` when
 * nothing is stored, and an empty string when the account is known to have no address.
 */
interface RecoveryEmailVault {
    suspend fun load(): String?
    suspend fun save(address: String)
    suspend fun clear()
}

/** A key in the sealed session store, so it shares its Keystore key and its wipe when that key is lost. */
class SealedRecoveryEmailVault @Inject constructor(
    @param:AuthPrefs private val dataStore: DataStore<Preferences>,
) : RecoveryEmailVault {

    private val addressKey = stringPreferencesKey("recovery_email")

    override suspend fun load(): String? = dataStore.data.first()[addressKey]

    override suspend fun save(address: String) {
        dataStore.edit { it[addressKey] = address }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(addressKey) }
    }
}
