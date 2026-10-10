package com.ipb.castelobranco.features.bible.data.repository

import com.ipb.castelobranco.core.data.local.SnapshotStorage
import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.features.bible.data.dto.BibleBookDto
import com.ipb.castelobranco.features.bible.data.local.BiblePreferences
import com.ipb.castelobranco.features.bible.domain.model.BibleBook
import com.ipb.castelobranco.features.bible.domain.model.BibleReadingPosition
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BibleRepositoryImpl @Inject constructor(
    private val caches: Map<BibleTranslation, @JvmSuppressWildcards SnapshotCache<List<BibleBookDto>>>,
    private val preferences: BiblePreferences,
    private val storage: SnapshotStorage,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BibleRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _cachedTranslations = MutableStateFlow<Set<BibleTranslation>>(emptySet())
    override val cachedTranslationsFlow: StateFlow<Set<BibleTranslation>> = _cachedTranslations.asStateFlow()

    /** Whether [_cachedTranslations] already reflects the disk; before that, "absent" is not known yet. */
    private val cachedTranslationsKnown = MutableStateFlow(false)

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
     * A whole translation weighs megabytes in the heap, so it is decoded only while someone collects (the
     * Bible graph) and dropped [BOOKS_STOP_TIMEOUT_MS] after the last collector leaves: the replay resets to
     * `null` instead of holding the books for the rest of the session. Decodes again when the active
     * translation changes or reaches the disk (download), and empties on `clearAll()`.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override val booksFlow: StateFlow<List<BibleBook>?> = flow {
        cachedTranslationsKnown.first { it }
        combine(activeTranslationFlow, _cachedTranslations) { translation, cached ->
            translation to (translation in cached)
        }.collect { emit(it) }
    }
        .distinctUntilChanged()
        .transformLatest { (translation, onDisk) ->
            if (!onDisk) {
                emit(emptyList())
                return@transformLatest
            }
            emit(null)
            val books = caches[translation]?.load()?.map { it.toDomain() }
            // A corrupt file was deleted by load(): the translation is no longer on disk.
            if (books == null) refreshCachedTranslations()
            emit(books.orEmpty())
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(BOOKS_STOP_TIMEOUT_MS, replayExpirationMillis = 0),
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
        // booksFlow decodes the new translation once activeTranslationFlow emits it.
    }

    override suspend fun savePosition(bookAbbrev: String, chapter: Int, verse: Int) {
        preferences.setPosition(bookAbbrev, chapter, verse)
    }

    override suspend fun setFontSize(size: Float) {
        preferences.setFontSize(size)
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        BibleTranslation.entries.forEach { translation ->
            caches[translation]?.clear()
        }
        preferences.resetAll()
        _cachedTranslations.value = emptySet()
    }

    /** Checks the files only: decoding a whole Bible to learn that it exists costs megabytes per translation. */
    private suspend fun refreshCachedTranslations() {
        val present = mutableSetOf<BibleTranslation>()
        for (t in BibleTranslation.entries) {
            val cache = caches[t] ?: continue
            if (cache.exists()) present.add(t)
        }
        _cachedTranslations.value = present
        cachedTranslationsKnown.value = true
    }

    private companion object {
        /** Survives a configuration change without decoding the Bible again. */
        const val BOOKS_STOP_TIMEOUT_MS = 5_000L
    }
}

private fun BibleBookDto.toDomain(): BibleBook =
    BibleBook(abbrev = abbrev, name = name, chapters = chapters)
