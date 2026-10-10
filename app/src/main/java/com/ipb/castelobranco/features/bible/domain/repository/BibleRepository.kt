package com.ipb.castelobranco.features.bible.domain.repository

import com.ipb.castelobranco.features.bible.domain.model.BibleBook
import com.ipb.castelobranco.features.bible.domain.model.BibleReadingPosition
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositório da Bíblia.
 *
 * Mantém em memória a tradução ativa, traduções já baixadas, posição
 * de leitura e tamanho da fonte. Persiste em [BiblePreferences] +
 * SnapshotStorage (filesDir/snapshots/bible_*.json).
 */
interface BibleRepository {
    /**
     * Livros da tradução ativa, decodificados só enquanto alguém coleta e soltos da memória logo depois.
     * `null` = ainda decodificando; vazio = tradução não está no aparelho.
     */
    val booksFlow: StateFlow<List<BibleBook>?>
    val activeTranslationFlow: StateFlow<BibleTranslation>
    val cachedTranslationsFlow: StateFlow<Set<BibleTranslation>>
    val positionFlow: StateFlow<BibleReadingPosition>
    val fontSizeFlow: StateFlow<Float>

    /** Detecta as traduções baixadas (só checa os arquivos, não decodifica). */
    suspend fun preload()

    /** Troca tradução ativa (e recarrega [booksFlow]). Persiste preferência. */
    suspend fun setActiveTranslation(translation: BibleTranslation)

    suspend fun savePosition(bookAbbrev: String, chapter: Int, verse: Int)

    suspend fun setFontSize(size: Float)

    /** Apaga JSON cacheado de TODAS as traduções e reseta a posição/preferência. */
    suspend fun clearAll()
}
