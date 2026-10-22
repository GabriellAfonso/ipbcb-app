package com.ipb.castelobranco.features.bible.data.repository

import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.features.bible.data.local.BibleLocalStore
import com.ipb.castelobranco.features.bible.data.local.BiblePreferences
import com.ipb.castelobranco.features.bible.domain.model.BibleBook
import com.ipb.castelobranco.features.bible.domain.model.BibleBookText
import com.ipb.castelobranco.features.bible.domain.model.BibleReadingPosition
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import com.ipb.castelobranco.features.bible.domain.model.resolve
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BibleRepositoryImpl @Inject constructor(
    private val store: BibleLocalStore,
    private val preferences: BiblePreferences,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BibleRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _cachedTranslations = MutableStateFlow<Set<BibleTranslation>>(emptySet())
    override val cachedTranslationsFlow: StateFlow<Set<BibleTranslation>> = _cachedTranslations.asStateFlow()

    /** Whether [_cachedTranslations] already reflects the disk; before that, "absent" is not known yet. */
    private val cachedTranslationsKnown = MutableStateFlow(false)

    /** The init check and `preload()` may overlap; the old-file conversion must run once. */
    private val refreshLock = Mutex()

    override val activeTranslationFlow: StateFlow<BibleTranslation> =
        preferences.translationFlow.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = BibleTranslation.Default,
        )

    override val positionFlow: StateFlow<BibleReadingPosition> =
        preferences.positionFlow.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = BibleReadingPosition.default(),
        )

    override val fontSizeFlow: StateFlow<Float> =
        preferences.fontSizeFlow.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = BiblePreferences.DEFAULT_FONT_SIZE,
        )

    /**
     * The index of the active translation, kept for the whole session (a few KB): names and chapter/verse counts
     * are there before any text. Keeps the translation it belongs to, so a book is never read from the wrong one.
     * On a translation change the previous index stays until the new one arrives.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val index: StateFlow<LoadedIndex?> = flow {
        cachedTranslationsKnown.first { it }
        combine(activeTranslationFlow, _cachedTranslations) { translation, cached ->
            translation to (translation in cached)
        }.collect { emit(it) }
    }
        .distinctUntilChanged()
        .transformLatest { (translation, onDisk) ->
            if (!onDisk) {
                emit(LoadedIndex(translation, emptyList()))
                return@transformLatest
            }
            val books = store.loadIndex(translation)
            // A corrupt index was deleted: the translation is no longer on disk.
            if (books == null) refreshCachedTranslations()
            emit(LoadedIndex(translation, books.orEmpty()))
        }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = null)

    override val booksFlow: StateFlow<List<BibleBook>?> = index
        .map { it?.books }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = null)

    /**
     * Text of the position's book only, read while someone collects (the Bible graph) and dropped
     * [BOOK_STOP_TIMEOUT_MS] after the last collector leaves. A chapter change in the same book reads nothing;
     * on a book or translation change the previous text stays until the new one arrives.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentBookFlow: StateFlow<BibleBookText?> =
        combine(index, positionFlow) { loaded, position ->
            val books = loaded?.books ?: return@combine null
            val book = books.resolve(position.bookAbbrev) ?: return@combine null
            BookKey(loaded.translation, books.indexOf(book), book.abbrev)
        }
            .distinctUntilChanged()
            .transformLatest { key ->
                if (key == null) {
                    emit(null)
                    return@transformLatest
                }
                val text = store.loadBook(key.translation, key.position, key.abbrev)
                // A corrupt book took its translation with it.
                if (text == null) refreshCachedTranslations()
                emit(text)
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(BOOK_STOP_TIMEOUT_MS, replayExpirationMillis = 0),
                initialValue = null,
            )

    init {
        // File checks only: the reader must know what is on disk without waiting for the boot preload.
        scope.launch { refreshCachedTranslations() }
    }

    override suspend fun preload() = withContext(ioDispatcher) {
        refreshCachedTranslations()
    }

    override suspend fun setActiveTranslation(translation: BibleTranslation) {
        preferences.setTranslation(translation)
        // The index and the current book follow once activeTranslationFlow emits it.
    }

    override suspend fun savePosition(bookAbbrev: String, chapter: Int, verse: Int) {
        preferences.setPosition(bookAbbrev, chapter, verse)
    }

    override suspend fun setFontSize(size: Float) {
        preferences.setFontSize(size)
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        BibleTranslation.entries.forEach { store.clear(it) }
        preferences.resetAll()
        _cachedTranslations.value = emptySet()
    }

    override suspend fun isAutoDownloadPaused(): Boolean = preferences.isAutoDownloadPaused()

    override suspend fun setAutoDownloadPaused(paused: Boolean) {
        preferences.setAutoDownloadPaused(paused)
    }

    override suspend fun diskUsageBytes(): Long =
        BibleTranslation.entries.sumOf { store.sizeBytes(it) }

    /** Converts old single-file translations, then checks the index files only: no text is decoded. */
    private suspend fun refreshCachedTranslations() = refreshLock.withLock {
        store.migrateLegacy()
        _cachedTranslations.value = BibleTranslation.entries.filter { store.exists(it) }.toSet()
        cachedTranslationsKnown.value = true
    }

    private data class LoadedIndex(val translation: BibleTranslation, val books: List<BibleBook>)

    private data class BookKey(val translation: BibleTranslation, val position: Int, val abbrev: String)

    private companion object {
        /** Survives a configuration change without reading the book again. */
        const val BOOK_STOP_TIMEOUT_MS = 5_000L
    }
}
