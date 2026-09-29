package com.ipb.castelobranco.features.gallery.data.local

import android.content.Context
import com.ipb.castelobranco.core.data.local.StorageDirConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Moves the gallery from the first layout (`gallery/{albumId}/{photoId}.{ext}` plus a `{photoId}.json`
 * per photo) to the flat one, without downloading anything again. Orphans — photos deleted on the server
 * since — are not handled here: the first sync after it is a full read, and the sync deletes every
 * original that is not in the index.
 *
 * Every step is idempotent, so a run killed halfway finishes on the next one.
 */
class GalleryLegacyMigration @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: GalleryPreferences,
) {

    suspend fun runIfNeeded() {
        if (preferences.layoutVersion() >= FLAT_LAYOUT_VERSION) return
        moveLegacyFiles()
        preferences.setLayoutVersion(FLAT_LAYOUT_VERSION)
    }

    fun moveLegacyFiles() {
        val root = File(context.filesDir, StorageDirConstants.GALLERY)
        val originals = File(root, GalleryMediaStore.ORIGINALS_DIR)
        val albumDirs = root.listFiles()
            ?.filter { it.isDirectory && it.name.toLongOrNull() != null }
            .orEmpty()

        for (albumDir in albumDirs) {
            albumDir.listFiles()?.filter { it.isFile }?.forEach { file ->
                if (file.extension.equals(METADATA_EXTENSION, ignoreCase = true)) {
                    file.delete()
                } else {
                    moveInto(file, originals)
                }
            }
            albumDir.deleteRecursively()
        }
    }

    private fun moveInto(file: File, dir: File) {
        val target = File(dir, file.name)
        if (target.exists()) {
            file.delete()
            return
        }
        dir.mkdirs()
        if (!file.renameTo(target)) {
            file.copyTo(target, overwrite = true)
            file.delete()
        }
    }

    companion object {
        const val FLAT_LAYOUT_VERSION = 2
        private const val METADATA_EXTENSION = "json"
    }
}
