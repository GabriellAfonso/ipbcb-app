package com.ipb.castelobranco.features.admin.members.data.photo

import com.ipb.castelobranco.core.data.security.AeadCipher
import com.ipb.castelobranco.core.data.security.UndecryptableException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.URI
import java.security.MessageDigest

/** A member photo as served, with what is needed to revalidate it. */
class CachedPhoto(val etag: String?, val mimeType: String, val bytes: ByteArray)

/**
 * Member photos on disk, one sealed file per photo in [dir] (`no_backup/member_photos`). File
 * names are the SHA-256 of the URL path, so no URL, id or name is ever readable. Least recently
 * used photos go first once the directory passes [maxBytes]. Format:
 * `specs/012-encrypted-session-photo-cache/contracts/on-device-storage.md`.
 */
class EncryptedMemberPhotoCache(
    private val dir: File,
    private val cipher: AeadCipher,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()

    suspend fun get(url: String): CachedPhoto? = locked {
        val key = keyOf(url)
        val file = File(dir, key)
        if (!file.exists()) return@locked null
        try {
            val photo = unpack(cipher.decrypt(file.readBytes(), associatedData(key)))
            file.setLastModified(nowMillis())
            photo
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Lost key, tampered or truncated file: drop it and download again.
            Timber.w("Member photo %s unreadable (%s)", key.take(LOG_KEY_CHARS), e.javaClass.simpleName)
            file.delete()
            null
        }
    }

    suspend fun put(url: String, photo: CachedPhoto) = locked {
        dir.mkdirs()
        val key = keyOf(url)
        val temp = File(dir, key + TEMP_SUFFIX)
        temp.writeBytes(cipher.encrypt(pack(photo), associatedData(key)))
        val target = File(dir, key)
        if (!temp.renameTo(target)) {
            target.delete()
            temp.renameTo(target)
        }
        target.setLastModified(nowMillis())
        evict()
    }

    suspend fun remove(url: String) = locked {
        File(dir, keyOf(url)).delete()
        Unit
    }

    suspend fun retainOnly(urls: Set<String>) = locked {
        val keep = urls.mapTo(HashSet(), ::keyOf)
        entries().filterNot { it.name in keep }.forEach { it.delete() }
    }

    suspend fun wipe() = locked {
        dir.deleteRecursively()
        cipher.destroyKey()
    }

    private fun evict() {
        val files = entries().sortedBy { it.lastModified() }
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= maxBytes) break
            total -= file.length()
            file.delete()
        }
    }

    private fun entries(): List<File> =
        dir.listFiles()?.filter { it.isFile && !it.name.endsWith(TEMP_SUFFIX) }.orEmpty()

    private suspend fun <T> locked(block: suspend () -> T): T =
        withContext(io) { mutex.withLock { block() } }

    private fun associatedData(key: String): ByteArray = (AAD_PREFIX + key).encodeToByteArray()

    private fun pack(photo: CachedPhoto): ByteArray {
        val out = ByteArrayOutputStream(photo.bytes.size + HEADER_ESTIMATE)
        DataOutputStream(out).use { data ->
            data.writeUTF(photo.etag.orEmpty())
            data.writeUTF(photo.mimeType)
            data.write(photo.bytes)
        }
        return out.toByteArray()
    }

    private fun unpack(plain: ByteArray): CachedPhoto = DataInputStream(plain.inputStream()).use { data ->
        val etag = data.readUTF().ifEmpty { null }
        val mime = data.readUTF()
        CachedPhoto(etag = etag, mimeType = mime, bytes = data.readBytes())
    }

    companion object {
        const val DIRECTORY = "member_photos"
        const val DEFAULT_MAX_BYTES = 50L * 1024 * 1024

        private const val TEMP_SUFFIX = ".tmp"
        private const val AAD_PREFIX = "member_photo:"
        private const val HEADER_ESTIMATE = 128
        private const val LOG_KEY_CHARS = 8

        /** SHA-256 of the URL path, in hex: the host may change, the photo name does not. */
        fun keyOf(url: String): String {
            val path = runCatching { URI(url).path }.getOrNull()?.takeIf { it.isNotEmpty() } ?: url
            return MessageDigest.getInstance("SHA-256")
                .digest(path.encodeToByteArray())
                .joinToString("") { "%02x".format(it) }
        }
    }
}
