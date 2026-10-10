package com.ipb.castelobranco.core.data.security

import androidx.datastore.core.CorruptionException
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ipb.castelobranco.core.testing.FakeAeadCipher
import kotlinx.coroutines.test.runTest
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedSerializerTest {

    private val key = stringPreferencesKey("auth_tokens")
    private val cipher = FakeAeadCipher()
    private val serializer = EncryptedSerializer(PreferencesSerializer, cipher, "auth_prefs")

    private suspend fun sealed(token: String): ByteArray =
        Buffer().also { serializer.writeTo(preferencesOf(key to token), it) }.readByteArray()

    @Test
    fun `round trip keeps the value and hides it`() = runTest {
        val bytes = sealed("secret-token")

        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("secret-token"))
        assertEquals("secret-token", serializer.readFrom(Buffer().write(bytes))[key])
    }

    @Test
    fun `empty file reads as empty preferences`() = runTest {
        assertTrue(serializer.readFrom(Buffer()).asMap().isEmpty())
    }

    @Test(expected = CorruptionException::class)
    fun `tampered byte is corruption`() = runTest {
        val bytes = sealed("secret-token")
        bytes[bytes.lastIndex] = (bytes.last() + 1).toByte()
        serializer.readFrom(Buffer().write(bytes))
    }

    @Test(expected = CorruptionException::class)
    fun `lost key is corruption`() = runTest {
        val bytes = sealed("secret-token")
        cipher.loseKey()
        serializer.readFrom(Buffer().write(bytes))
    }

    @Test(expected = CorruptionException::class)
    fun `other label is corruption`() = runTest {
        val bytes = sealed("secret-token")
        EncryptedSerializer(PreferencesSerializer, cipher, "other").readFrom(Buffer().write(bytes))
    }
}
