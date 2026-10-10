package com.ipb.castelobranco.core.testing

import com.ipb.castelobranco.core.data.security.AeadCipher
import com.ipb.castelobranco.core.data.security.UndecryptableException

/**
 * A reversible stand-in for the Keystore cipher: XOR with a key byte, framed by a version byte and
 * a checksum over plain + associated data, so tampering, a wrong label or a destroyed key fail the
 * same way the real cipher does.
 */
class FakeAeadCipher : AeadCipher {

    private var keyByte: Int = INITIAL_KEY
    private var keyLost = false
    var destroyed = 0
        private set
    var failNextEncrypt = false

    /** The next [decrypt] behaves as if the key vanished from the key store. */
    fun loseKey() {
        keyLost = true
    }

    override fun encrypt(plain: ByteArray, associatedData: ByteArray): ByteArray {
        if (failNextEncrypt) {
            failNextEncrypt = false
            error("encrypt failed")
        }
        keyLost = false
        val body = ByteArray(plain.size) { (plain[it].toInt() xor keyByte).toByte() }
        return byteArrayOf(VERSION, checksum(plain, associatedData)) + body
    }

    override fun decrypt(sealed: ByteArray, associatedData: ByteArray): ByteArray {
        if (keyLost || sealed.size < 2 || sealed[0] != VERSION) throw UndecryptableException()
        val plain = ByteArray(sealed.size - 2) { (sealed[it + 2].toInt() xor keyByte).toByte() }
        if (checksum(plain, associatedData) != sealed[1]) throw UndecryptableException()
        return plain
    }

    override fun destroyKey() {
        destroyed++
        keyByte = (keyByte + 1) and BYTE_MASK
    }

    private fun checksum(plain: ByteArray, associatedData: ByteArray): Byte =
        (plain.sumOf { it.toInt() } * PRIME + associatedData.sumOf { it.toInt() } + keyByte).toByte()

    private companion object {
        const val VERSION: Byte = 1
        const val INITIAL_KEY = 0x5A
        const val BYTE_MASK = 0xFF
        const val PRIME = 31
    }
}
