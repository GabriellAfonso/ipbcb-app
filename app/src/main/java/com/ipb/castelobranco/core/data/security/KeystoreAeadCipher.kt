package com.ipb.castelobranco.core.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM with a non-exportable key held by `AndroidKeyStore` under [alias] (hardware-backed
 * when the device has it). The key is created on the first [encrypt]; [decrypt] never creates one.
 *
 * Sealed format: `[version][12-byte IV][ciphertext ‖ 128-bit tag]` —
 * `specs/012-encrypted-session-photo-cache/contracts/on-device-storage.md`.
 */
class KeystoreAeadCipher(private val alias: String) : AeadCipher {

    private val lock = Any()

    override fun encrypt(plain: ByteArray, associatedData: ByteArray): ByteArray = synchronized(lock) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, existingKey() ?: createKey())
        cipher.updateAAD(associatedData)
        val iv = cipher.iv
        check(iv.size == IV_SIZE) { "Unexpected IV size ${iv.size}" }
        val body = cipher.doFinal(plain)
        ByteArray(1 + IV_SIZE + body.size).also { out ->
            out[0] = VERSION
            iv.copyInto(out, destinationOffset = 1)
            body.copyInto(out, destinationOffset = 1 + IV_SIZE)
        }
    }

    override fun decrypt(sealed: ByteArray, associatedData: ByteArray): ByteArray = synchronized(lock) {
        if (sealed.size < 1 + IV_SIZE + TAG_BYTES || sealed[0] != VERSION) throw UndecryptableException()
        try {
            val key = existingKey() ?: throw UndecryptableException()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, sealed, 1, IV_SIZE))
            cipher.updateAAD(associatedData)
            cipher.doFinal(sealed, 1 + IV_SIZE, sealed.size - 1 - IV_SIZE)
        } catch (e: UndecryptableException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // GeneralSecurityException, KeyStoreException, ProviderException (manufacturer faults)…
            throw UndecryptableException(e)
        }
    }

    override fun destroyKey() = synchronized(lock) {
        runCatching { keyStore().deleteEntry(alias) }
            .onFailure { Timber.w(it, "Key deletion failed") }
        Unit
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    private fun existingKey(): SecretKey? = keyStore().getKey(alias, null) as? SecretKey

    private fun createKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_BITS)
            .setRandomizedEncryptionRequired(true)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            .apply { init(spec) }
            .generateKey()
    }

    companion object {
        const val SESSION_KEY_ALIAS = "ipbcb_session_v1"
        const val MEMBER_PHOTOS_KEY_ALIAS = "ipbcb_member_photos_v1"

        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val VERSION: Byte = 1
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
        private const val TAG_BYTES = TAG_BITS / 8
        private const val KEY_BITS = 256
    }
}
