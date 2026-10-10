package com.ipb.castelobranco.core.data.security

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

/**
 * The session store: a Preferences DataStore whose file is only ever a sealed blob
 * (`datastore/auth_prefs.enc.preferences_pb`). On the first read it absorbs the old plain-text
 * file; an unreadable store becomes empty and goes through [SessionRecovery].
 */
object EncryptedAuthPrefs {

    private const val DATASTORE_DIR = "datastore"
    private const val PLAIN_EXTENSION = ".preferences_pb"

    fun create(
        filesDir: File,
        cipher: AeadCipher,
        recovery: SessionRecovery,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    ): DataStore<Preferences> {
        val dir = File(filesDir, DATASTORE_DIR)
        val encrypted = File(dir, AuthPrefsFiles.ENCRYPTED_FILE)
        val plain = File(dir, AuthPrefsFiles.PLAIN_NAME + PLAIN_EXTENSION)
        return PreferenceDataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = EncryptedSerializer(PreferencesSerializer, cipher, AuthPrefsFiles.CIPHER_LABEL),
                producePath = { encrypted.absoluteFile.toOkioPath() },
            ),
            corruptionHandler = ReplaceFileCorruptionHandler {
                recovery.onSessionUnreadable()
                emptyPreferences()
            },
            migrations = listOf(PlaintextAuthPrefsMigration(plain)),
            scope = scope,
        )
    }
}
