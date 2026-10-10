package com.ipb.castelobranco.core.data.security

/**
 * Authenticated encryption for data the app keeps on the device. [encrypt] seals bytes with a key
 * that never leaves the platform key store; [decrypt] opens them only when the same
 * [associatedData] is given, so a blob cannot be moved to another file and still open.
 *
 * Every failure to open — key missing or broken, blob tampered, unknown format — is an
 * [UndecryptableException]. It never leaves the data layer.
 */
interface AeadCipher {
    fun encrypt(plain: ByteArray, associatedData: ByteArray): ByteArray

    /** @throws UndecryptableException when the blob cannot be opened */
    fun decrypt(sealed: ByteArray, associatedData: ByteArray): ByteArray

    /** Deletes the key; the next [encrypt] creates a new one and old blobs never open again. */
    fun destroyKey()
}

class UndecryptableException(cause: Throwable? = null) : Exception(cause)
