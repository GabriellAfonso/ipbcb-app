package com.ipb.castelobranco.features.bible.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ipb.castelobranco.core.di.SettingsPrefs
import com.ipb.castelobranco.features.bible.domain.model.BibleReadingPosition
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BiblePreferences @Inject constructor(
    @param:SettingsPrefs private val dataStore: DataStore<Preferences>,
) {
    companion object {
        const val DEFAULT_FONT_SIZE: Float = 18f
        const val MIN_FONT_SIZE: Float = 14f
        const val MAX_FONT_SIZE: Float = 32f
    }

    private val translationKey  = stringPreferencesKey("bible_translation")
    private val lastBookKey     = stringPreferencesKey("bible_last_book_abbrev")
    private val lastChapterKey  = intPreferencesKey("bible_last_chapter")
    private val lastVerseKey    = intPreferencesKey("bible_last_verse")
    private val fontSizeKey     = floatPreferencesKey("bible_font_size")

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

    val fontSizeFlow: Flow<Float> =
        dataStore.data.map { prefs -> prefs[fontSizeKey] ?: DEFAULT_FONT_SIZE }

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

    suspend fun setFontSize(size: Float) {
        dataStore.edit { prefs -> prefs[fontSizeKey] = size.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE) }
    }

    suspend fun resetAll() {
        dataStore.edit { prefs ->
            prefs.remove(translationKey)
            prefs.remove(lastBookKey)
            prefs.remove(lastChapterKey)
            prefs.remove(lastVerseKey)
            prefs.remove(fontSizeKey)
        }
    }
}
