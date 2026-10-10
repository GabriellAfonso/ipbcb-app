package com.ipb.castelobranco.core.data.security

import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ipb.castelobranco.core.testing.FakeAeadCipher
import com.ipb.castelobranco.features.auth.data.local.TokenStorage
import com.ipb.castelobranco.features.auth.domain.model.AuthTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.buffer
import okio.sink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EncryptedAuthPrefsStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val cipher = FakeAeadCipher()
    private val recovery = SessionRecovery(cipher)
    private val json = Json { ignoreUnknownKeys = true }
    private val tokens = AuthTokens(access = "access-abc", refresh = "refresh-xyz")

    private val datastoreDir get() = File(folder.root, "datastore")
    private val encryptedFile get() = File(datastoreDir, AuthPrefsFiles.ENCRYPTED_FILE)
    private val plainFile get() = File(datastoreDir, "auth_prefs.preferences_pb")

    /** Runs [block] against a fresh store instance, cancelling it afterwards so the next can open the file. */
    private suspend fun <T> withStorage(block: suspend (TokenStorage) -> T): T {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = EncryptedAuthPrefs.create(folder.root, cipher, recovery, scope)
        return try {
            block(TokenStorage(store, json))
        } finally {
            scope.coroutineContext.job.cancelAndJoin()
        }
    }

    private suspend fun TokenStorage.read(): AuthTokens? = tokensFlow.first()

    @Test
    fun `tokens are written sealed and read back by a new instance`() = runTest {
        withStorage { it.save(tokens) }

        val raw = String(encryptedFile.readBytes(), Charsets.ISO_8859_1)
        assertFalse(raw.contains("access-abc"))
        assertFalse(raw.contains("refresh-xyz"))
        assertEquals(tokens, withStorage { it.read() })
    }

    @Test
    fun `plain session is migrated and the plain file removed`() = runTest {
        datastoreDir.mkdirs()
        val plainJson = json.encodeToString(AuthTokens.serializer(), tokens)
        plainFile.sink().buffer().use {
            PreferencesSerializer.writeTo(
                preferencesOf(stringPreferencesKey(AuthPrefsFiles.AUTH_TOKENS_KEY) to plainJson),
                it,
            )
        }

        assertEquals(tokens, withStorage { it.tokensFlow.filterNotNull().first() })
        assertFalse(plainFile.exists())
        assertTrue(encryptedFile.exists())
    }

    @Test
    fun `lost key reads as signed out and asks for a wipe`() = runTest {
        withStorage { it.save(tokens) }
        cipher.loseKey()

        assertNull(withStorage { it.read() })
        assertTrue(recovery.pendingWipe.value)
        assertEquals(1, cipher.destroyed)
    }

    @Test
    fun `corrupt file reads as signed out and a new session works`() = runTest {
        withStorage { it.save(tokens) }
        encryptedFile.writeBytes(byteArrayOf(0x09, 0x08, 0x07))

        assertNull(withStorage { it.read() })
        assertTrue(recovery.pendingWipe.value)

        withStorage { it.save(tokens) }
        assertEquals(tokens, withStorage { it.read() })
    }
}
