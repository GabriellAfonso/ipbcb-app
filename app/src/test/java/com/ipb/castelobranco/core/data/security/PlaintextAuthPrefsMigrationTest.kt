package com.ipb.castelobranco.core.data.security

import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.test.runTest
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

class PlaintextAuthPrefsMigrationTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val key = stringPreferencesKey(AuthPrefsFiles.AUTH_TOKENS_KEY)

    private fun oldFile(): File = File(folder.root, "auth_prefs.preferences_pb")

    private suspend fun writeOld(tokens: String): File = oldFile().also { file ->
        file.sink().buffer().use { PreferencesSerializer.writeTo(preferencesOf(key to tokens), it) }
    }

    @Test
    fun `no old file means nothing to migrate`() = runTest {
        assertFalse(PlaintextAuthPrefsMigration(oldFile()).shouldMigrate(emptyPreferences()))
    }

    @Test
    fun `old tokens move into an empty store and the old file is deleted`() = runTest {
        val file = writeOld("old-tokens")
        val migration = PlaintextAuthPrefsMigration(file)

        assertTrue(migration.shouldMigrate(emptyPreferences()))
        assertEquals("old-tokens", migration.migrate(emptyPreferences())[key])

        migration.cleanUp()
        assertFalse(file.exists())
    }

    @Test
    fun `newer encrypted tokens are kept over the leftover old file`() = runTest {
        val migration = PlaintextAuthPrefsMigration(writeOld("old-tokens"))

        val result = migration.migrate(preferencesOf(key to "renewed-tokens"))

        assertEquals("renewed-tokens", result[key])
    }

    @Test
    fun `corrupt old file starts signed out without throwing`() = runTest {
        val file = oldFile().apply { writeBytes(byteArrayOf(0x7F, 0x01, 0x02)) }

        assertNull(PlaintextAuthPrefsMigration(file).migrate(emptyPreferences())[key])
    }
}
