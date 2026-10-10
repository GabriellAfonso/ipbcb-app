package com.ipb.castelobranco.core.data.security

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.stringPreferencesKey
import okio.buffer
import okio.source
import timber.log.Timber
import java.io.File

/** Names shared by the session store, its migration and the token storage. */
object AuthPrefsFiles {
    /** The session store before encryption — plain protobuf. Must not survive the migration. */
    const val PLAIN_NAME = "auth_prefs"

    /** The encrypted session store, under `files/datastore/`. */
    const val ENCRYPTED_FILE = "auth_prefs.enc.preferences_pb"

    /** Associated data that binds the sealed blob to this store. */
    const val CIPHER_LABEL = "auth_prefs"

    const val AUTH_TOKENS_KEY = "auth_tokens"
}

/**
 * Moves a signed-in session from the plain-text store into the encrypted one, so nobody has to sign
 * in again after the update. DataStore runs it before the first read and calls [cleanUp] only after
 * the migrated data is written, so the plain file goes only once the encrypted copy exists.
 *
 * Safe to repeat: if the encrypted store already holds tokens (killed after writing, before
 * cleanup, then the session was renewed) those are newer and are kept. An unreadable old file
 * counts as signed out — never a crash.
 */
class PlaintextAuthPrefsMigration(private val oldFile: File) : DataMigration<Preferences> {

    private val tokensKey = stringPreferencesKey(AuthPrefsFiles.AUTH_TOKENS_KEY)

    override suspend fun shouldMigrate(currentData: Preferences): Boolean = oldFile.exists()

    override suspend fun migrate(currentData: Preferences): Preferences {
        if (currentData[tokensKey] != null) return currentData
        return runCatching { oldFile.source().buffer().use { PreferencesSerializer.readFrom(it) } }
            .onFailure { Timber.w("Plain session store unreadable; starting signed out") }
            .getOrDefault(currentData)
    }

    override suspend fun cleanUp() {
        if (oldFile.exists() && !oldFile.delete()) Timber.w("Plain session store could not be deleted")
    }
}
