package com.ipb.castelobranco.features.bible.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ipb.castelobranco.core.data.local.TextScaleStore
import com.ipb.castelobranco.core.di.SettingsPrefs
import com.ipb.castelobranco.features.bible.domain.model.BibleReadingPosition
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BiblePreferences @Inject constructor(
    @param:SettingsPrefs private val dataStore: DataStore<Preferences>,
) {
    companion object {
        /** The reader's text at 100% (`specs/core/spec.md` §6.7.1). */
        const val BASE_FONT_SIZE: Float = 18f
    }

    private val translationKey  = stringPreferencesKey("bible_translation")
    private val lastBookKey     = stringPreferencesKey("bible_last_book_abbrev")
    private val lastChapterKey  = intPreferencesKey("bible_last_chapter")
    private val lastVerseKey    = intPreferencesKey("bible_last_verse")
    private val textScale = TextScaleStore(
        dataStore = dataStore,
        key = "bible_text_scale",
        legacySpKey = "bible_font_size",
        legacyBaseSp = BASE_FONT_SIZE,
    )

    // Outside resetAll(): deleting the Bible must not undo its own pause.
    private val autoDownloadPausedKey = booleanPreferencesKey("bible_auto_download_paused")

    val translationFlow: Flow<BibleTranslation> =
        dataStore.data.map { prefs ->
            BibleTranslation.fromCode(prefs[translationKey])
        }

    val positionFlow: Flow<BibleReadingPosition> =
        dataStore.data.map { prefs ->
            BibleReadingPosition(
                bookAbbrev = prefs[lastBookKey] ?: BibleReadingPosition.DEFAULT_BOOK_ABBREV,
                chapter    = prefs[lastChapterKey] ?: BibleReadingPosition.DEFAULT_CHAPTER,
                verse      = prefs[lastVerseKey] ?: BibleReadingPosition.DEFAULT_VERSE,
            )
        }

    /** Percent of [BASE_FONT_SIZE]. */
    val textScaleFlow: Flow<Int> = textScale.flow

    suspend fun setTranslation(translation: BibleTranslation) {
        dataStore.edit { prefs -> prefs[translationKey] = translation.code }
    }

    suspend fun setPosition(bookAbbrev: String, chapter: Int, verse: Int) {
        dataStore.edit { prefs ->
            prefs[lastBookKey]    = bookAbbrev
            prefs[lastChapterKey] = chapter
            prefs[lastVerseKey]   = verse
        }
    }

    suspend fun setTextScale(percent: Int) = textScale.set(percent)

    suspend fun isAutoDownloadPaused(): Boolean = dataStore.data.first()[autoDownloadPausedKey] ?: false

    suspend fun setAutoDownloadPaused(paused: Boolean) {
        dataStore.edit { prefs -> prefs[autoDownloadPausedKey] = paused }
    }

    suspend fun resetAll() {
        dataStore.edit { prefs ->
            prefs.remove(translationKey)
            prefs.remove(lastBookKey)
            prefs.remove(lastChapterKey)
            prefs.remove(lastVerseKey)
            textScale.clear(prefs)
        }
    }
}
