package com.ipb.castelobranco.features.bible.data.local

import com.ipb.castelobranco.features.bible.data.dto.BibleBookDto
import com.ipb.castelobranco.features.bible.data.dto.BibleHeadingDto
import com.ipb.castelobranco.features.bible.data.dto.BibleIndexEntryDto
import com.ipb.castelobranco.features.bible.domain.model.BibleBook
import com.ipb.castelobranco.features.bible.domain.model.BibleBookText
import com.ipb.castelobranco.features.bible.domain.model.BibleHeading
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Each translation split per book under `[rootDir]/{naa,ara}/`: `index.json` (books without text) plus one
 * `NN.json` per book, named by its position in the index, and its `NN.headings.json`. Reading a chapter never
 * decodes more than its book. A `format` file holds the layout version: folders written before section headings
 * existed lack it and stay readable, without headings, until downloaded again.
 *
 * Also converts the old single-file snapshot (`[legacyDir]/bible_{naa,ara}.json`) left by earlier versions.
 */
class BibleLocalStore(
    private val rootDir: File,
    private val legacyDir: File,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher,
) {

    private val indexSerializer = ListSerializer(BibleIndexEntryDto.serializer())
    private val chaptersSerializer = ListSerializer(ListSerializer(String.serializer()))
    private val headingsSerializer = ListSerializer(ListSerializer(BibleHeadingDto.serializer()))

    suspend fun exists(translation: BibleTranslation): Boolean = withContext(ioDispatcher) {
        indexFile(translation).exists()
    }

    /** On disk and in the current layout: nothing to download again. */
    suspend fun isCurrent(translation: BibleTranslation): Boolean = withContext(ioDispatcher) {
        indexFile(translation).exists() && formatVersion(dir(translation)) >= FORMAT_VERSION
    }

    /** The index of [translation], or `null` when it is absent or corrupt (a corrupt one is deleted). */
    suspend fun loadIndex(translation: BibleTranslation): List<BibleBook>? = withContext(ioDispatcher) {
        decodeOrDelete(translation, indexFile(translation), indexSerializer)
            ?.map { BibleBook(abbrev = it.abbrev, name = it.name, verseCounts = it.verseCounts) }
    }

    /** The text of the book at [position] in the index, or `null` when absent or corrupt. */
    suspend fun loadBook(translation: BibleTranslation, position: Int, abbrev: String): BibleBookText? =
        withContext(ioDispatcher) {
            val dir = dir(translation)
            val chapters = decodeOrDelete(translation, bookFile(dir, position), chaptersSerializer)
                ?: return@withContext null
            // An old-layout folder has no headings file: the book is read without headings. A corrupt one took the
            // translation with it, like a corrupt text file.
            val headingsFile = headingsFile(dir, position)
            val headings = if (headingsFile.exists()) {
                decodeOrDelete(translation, headingsFile, headingsSerializer) ?: return@withContext null
            } else {
                emptyList()
            }
            BibleBookText(abbrev = abbrev, chapters = chapters, headings = headings.toDomain())
        }

    /**
     * Splits the API response (`[{abbrev, name, chapters, headings}]`) read from [input], one book at a time, into a
     * temporary folder that replaces the translation's only once the index is written: never the whole translation in
     * memory, never a half-written translation in place. [markCurrent] `false` leaves the folder in the old layout
     * (a source that never had headings), so it is downloaded again.
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun import(
        translation: BibleTranslation,
        input: InputStream,
        markCurrent: Boolean = true,
    ): Unit = withContext(ioDispatcher) {
        val tmp = tempDir(translation).apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            val index = mutableListOf<BibleIndexEntryDto>()
            json.decodeToSequence(input, BibleBookDto.serializer(), DecodeSequenceMode.ARRAY_WRAPPED)
                .forEach { book ->
                    bookFile(tmp, index.size).writeText(json.encodeToString(chaptersSerializer, book.chapters))
                    headingsFile(tmp, index.size).writeText(json.encodeToString(headingsSerializer, book.headings))
                    index += BibleIndexEntryDto(book.abbrev, book.name, book.chapters.map { it.size })
                }
            if (index.isEmpty()) throw IOException("Bible ${translation.code} came without books")
            if (markCurrent) File(tmp, FORMAT_FILE).writeText(FORMAT_VERSION.toString())
            File(tmp, INDEX_FILE).writeText(json.encodeToString(indexSerializer, index))

            val target = dir(translation)
            target.deleteRecursively()
            if (!tmp.renameTo(target)) throw IOException("Could not move ${tmp.name} into place")
        } catch (e: Exception) {
            tmp.deleteRecursively()
            throw e
        }
    }

    /** Converts each old single-file translation into the per-book layout, then deletes the old file. */
    suspend fun migrateLegacy(): Unit = withContext(ioDispatcher) {
        for (translation in BibleTranslation.entries) {
            val legacy = legacyFile(translation)
            if (!legacy.exists()) continue
            if (!indexFile(translation).exists()) {
                // The old file predates headings: the converted folder stays outdated and is downloaded again.
                runCatching { legacy.inputStream().buffered().use { import(translation, it, markCurrent = false) } }
                    .onFailure { Timber.w(it, "Could not convert old Bible %s", translation.code) }
            }
            legacy.delete()
            legacyETagFile(translation).delete()
        }
    }

    suspend fun clear(translation: BibleTranslation): Unit = withContext(ioDispatcher) {
        dir(translation).deleteRecursively()
        tempDir(translation).deleteRecursively()
        legacyFile(translation).delete()
        legacyETagFile(translation).delete()
    }

    suspend fun sizeBytes(translation: BibleTranslation): Long = withContext(ioDispatcher) {
        dir(translation).walkTopDown().filter { it.isFile }.sumOf { it.length() } +
            legacyFile(translation).length() + legacyETagFile(translation).length()
    }

    private fun <T> decodeOrDelete(translation: BibleTranslation, file: File, serializer: KSerializer<T>): T? {
        if (!file.exists()) return null
        return try {
            json.decodeFromString(serializer, file.readText())
        } catch (e: SerializationException) {
            Timber.w(e, "Corrupt Bible %s (%s), deleting", translation.code, file.name)
            dir(translation).deleteRecursively()
            null
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Corrupt Bible %s (%s), deleting", translation.code, file.name)
            dir(translation).deleteRecursively()
            null
        } catch (e: IOException) {
            Timber.w(e, "Could not read Bible %s (%s)", translation.code, file.name)
            null
        }
    }

    private fun dir(translation: BibleTranslation) = File(rootDir, translation.code.lowercase())

    private fun tempDir(translation: BibleTranslation) = File(rootDir, "${translation.code.lowercase()}$TEMP_SUFFIX")

    private fun indexFile(translation: BibleTranslation) = File(dir(translation), INDEX_FILE)

    private fun bookFile(dir: File, position: Int) = File(dir, "${bookStem(position)}.json")

    private fun headingsFile(dir: File, position: Int) = File(dir, "${bookStem(position)}$HEADINGS_SUFFIX")

    private fun bookStem(position: Int) = position.toString().padStart(2, '0')

    /** `0` when the folder predates the `format` file (or it is unreadable): the old, headings-less layout. */
    private fun formatVersion(dir: File): Int =
        runCatching { File(dir, FORMAT_FILE).readText().trim().toInt() }.getOrDefault(0)

    private fun BibleHeadingDto.toDomain() = BibleHeading(verse = verse, level = level, title = title)

    private fun List<List<BibleHeadingDto>>.toDomain() = map { chapter -> chapter.map { it.toDomain() } }

    private fun legacyFile(translation: BibleTranslation) = File(legacyDir, "${translation.snapshotKey}.json")

    private fun legacyETagFile(translation: BibleTranslation) =
        File(legacyDir, "${translation.snapshotKey}_etag.txt")

    private companion object {
        const val INDEX_FILE = "index.json"
        const val TEMP_SUFFIX = ".tmp"
        const val HEADINGS_SUFFIX = ".headings.json"
        const val FORMAT_FILE = "format"

        /** 2 = with section headings. Bump when the layout changes so installed copies are downloaded again. */
        const val FORMAT_VERSION = 2
    }
}
