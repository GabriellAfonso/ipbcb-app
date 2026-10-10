package com.ipb.castelobranco.core.data.security

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource

/**
 * Wraps a DataStore serializer so the file holds only a sealed blob: [delegate] writes into memory,
 * [cipher] seals it with [label] as associated data. A blob that does not open becomes a
 * [CorruptionException], which DataStore hands to its corruption handler.
 */
class EncryptedSerializer<T>(
    private val delegate: OkioSerializer<T>,
    private val cipher: AeadCipher,
    label: String,
) : OkioSerializer<T> {

    private val associatedData = label.encodeToByteArray()

    override val defaultValue: T get() = delegate.defaultValue

    override suspend fun readFrom(source: BufferedSource): T {
        val sealed = source.readByteArray()
        if (sealed.isEmpty()) return defaultValue
        val plain = try {
            cipher.decrypt(sealed, associatedData)
        } catch (e: UndecryptableException) {
            throw CorruptionException(UNREADABLE, e)
        }
        return delegate.readFrom(Buffer().write(plain))
    }

    override suspend fun writeTo(t: T, sink: BufferedSink) {
        val plain = Buffer().also { delegate.writeTo(t, it) }.readByteArray()
        sink.write(cipher.encrypt(plain, associatedData))
    }

    private companion object {
        const val UNREADABLE = "Encrypted store cannot be opened"
    }
}
