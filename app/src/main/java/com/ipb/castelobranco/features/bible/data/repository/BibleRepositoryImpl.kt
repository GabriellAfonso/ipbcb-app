package com.ipb.castelobranco.features.bible.data.repository

import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.domain.model.TextScale
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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
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

    private val _outdatedTranslations = MutableStateFlow<Set<BibleTranslation>>(emptySet())
    override val outdatedTranslationsFlow: StateFlow<Set<BibleTranslation>> = _outdatedTranslations.asStateFlow()

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

    /**
     * The position just asked for, ahead of the disk: the reader moves now and the DataStore write follows. Once the
     * write lands both agree; [clearAll] drops it so the reset position shows.
     */
    private val requestedPosition = MutableStateFlow<BibleReadingPosition?>(null)

    override val positionFlow: StateFlow<BibleReadingPosition> =
        combine(preferences.positionFlow, requestedPosition) { saved, requested -> requested ?: saved }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = BibleReadingPosition.default(),
            )

    override val textScaleFlow: StateFlow<Int> =
        preferences.textScaleFlow.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = TextScale.DEFAULT,
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
     * Texts of the open book and of the two books around it (the Bible wraps: Revelation and Genesis are neighbours),
     * so the chapter arrows and the swipe cross a book boundary without reading the disk. Written only by
     * [currentBookFlow], whose blocks run one at a time; emptied when it stops.
     */
    private val bookCache = MutableStateFlow<Map<BookKey, BibleBookText>>(emptyMap())

    override val nearbyBooksFlow: StateFlow<List<BibleBookText>> = bookCache
        .map { it.values.toList() }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    /**
     * Text of the position's book, read while someone collects (the Bible graph) and dropped [BOOK_STOP_TIMEOUT_MS]
     * after the last collector leaves. Once it is out, the neighbouring books are read in the background into
     * [bookCache]. A chapter change in the same book reads nothing; on a book or translation change the previous text
     * stays until the new one arrives. When the translation leaves the old layout (a format upgrade finished), the
     * book is read again to pick up its headings.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentBookFlow: StateFlow<BibleBookText?> =
        combine(index, positionFlow, _outdatedTranslations) { loaded, position, outdated ->
            val books = loaded?.books ?: return@combine null
            val book = books.resolve(position.bookAbbrev) ?: return@combine null
            val at = books.indexOf(book)
            fun key(i: Int) = BookKey(loaded.translation, i, books[i].abbrev, loaded.translation in outdated)
            val around = listOf((at + 1) % books.size, (at - 1 + books.size) % books.size).distinct() - at
            BookRequest(open = key(at), neighbours = around.map(::key))
        }
            .distinctUntilChanged()
            .transformLatest { request ->
                if (request == null) {
                    bookCache.value = emptyMap()
                    emit(null)
                    return@transformLatest
                }
                val text = bookCache.value[request.open] ?: readBook(request.open)
                emit(text)
                val wanted = request.neighbours + request.open
                bookCache.update { cache ->
                    cache.filterKeys { it in wanted } + listOfNotNull(text?.let { request.open to it })
                }
                request.neighbours.filterNot(bookCache.value::containsKey).forEach { neighbour ->
                    readBook(neighbour)?.let { neighbourText -> bookCache.update { it + (neighbour to neighbourText) } }
                }
            }
            .onCompletion { bookCache.value = emptyMap() }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(BOOK_STOP_TIMEOUT_MS, replayExpirationMillis = 0),
                initialValue = null,
            )

    /** A corrupt book took its translation with it: the cached set is checked again. */
    private suspend fun readBook(key: BookKey): BibleBookText? =
        store.loadBook(key.translation, key.position, key.abbrev).also { if (it == null) refreshCachedTranslations() }

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
        requestedPosition.value = BibleReadingPosition(bookAbbrev, chapter, verse)
        preferences.setPosition(bookAbbrev, chapter, verse)
    }

    override suspend fun setTextScale(percent: Int) {
        preferences.setTextScale(percent)
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        BibleTranslation.entries.forEach { store.clear(it) }
        preferences.resetAll()
        requestedPosition.value = null
        _cachedTranslations.value = emptySet()
        _outdatedTranslations.value = emptySet()
    }

    override suspend fun isAutoDownloadPaused(): Boolean = preferences.isAutoDownloadPaused()

    override suspend fun setAutoDownloadPaused(paused: Boolean) {
        preferences.setAutoDownloadPaused(paused)
    }

    override suspend fun diskUsageBytes(): Long =
        BibleTranslation.entries.sumOf { store.sizeBytes(it) }

    /** Converts old single-file translations, then checks the index and format files only: no text is decoded. */
    private suspend fun refreshCachedTranslations() = refreshLock.withLock {
        store.migrateLegacy()
        val cached = BibleTranslation.entries.filter { store.exists(it) }.toSet()
        _outdatedTranslations.value = cached.filterNot { store.isCurrent(it) }.toSet()
        _cachedTranslations.value = cached
        cachedTranslationsKnown.value = true
    }

    private data class LoadedIndex(val translation: BibleTranslation, val books: List<BibleBook>)

    private data class BookRequest(val open: BookKey, val neighbours: List<BookKey>)

    private data class BookKey(
        val translation: BibleTranslation,
        val position: Int,
        val abbrev: String,
        val outdated: Boolean,
    )

    private companion object {
        /** Survives a configuration change without reading the book again. */
        const val BOOK_STOP_TIMEOUT_MS = 5_000L
    }
}
