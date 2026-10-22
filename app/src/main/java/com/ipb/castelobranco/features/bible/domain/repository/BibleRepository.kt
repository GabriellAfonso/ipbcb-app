package com.ipb.castelobranco.features.bible.domain.repository

import com.ipb.castelobranco.features.bible.domain.model.BibleBook
import com.ipb.castelobranco.features.bible.domain.model.BibleBookText
import com.ipb.castelobranco.features.bible.domain.model.BibleReadingPosition
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositório da Bíblia.
 *
 * Mantém em memória a tradução ativa, traduções já baixadas, o índice de livros, o texto do livro atual,
 * posição de leitura e tamanho da fonte. Persiste em [BiblePreferences] + `filesDir/bible/{naa,ara}/`
 * (um arquivo por livro).
 */
interface BibleRepository {
    /**
     * Índice da tradução ativa (livros sem texto), mantido pela sessão inteira.
     * `null` = ainda lendo; vazio = tradução não está no aparelho.
     */
    val booksFlow: StateFlow<List<BibleBook>?>

    /**
     * Texto do livro da posição atual, lido só enquanto alguém coleta e solto da memória logo depois.
     * `null` = nada lido ainda; ao mudar de livro, o anterior fica até o novo chegar.
     */
    val currentBookFlow: StateFlow<BibleBookText?>
    val activeTranslationFlow: StateFlow<BibleTranslation>
    val cachedTranslationsFlow: StateFlow<Set<BibleTranslation>>
    val positionFlow: StateFlow<BibleReadingPosition>
    val fontSizeFlow: StateFlow<Float>

    /** Converte o arquivo antigo, se houver, e detecta as traduções baixadas (só checa os arquivos). */
    suspend fun preload()

    /** Troca tradução ativa (e recarrega [booksFlow] e [currentBookFlow]). Persiste preferência. */
    suspend fun setActiveTranslation(translation: BibleTranslation)

    suspend fun savePosition(bookAbbrev: String, chapter: Int, verse: Int)

    suspend fun setFontSize(size: Float)

    /** Apaga os arquivos de TODAS as traduções e reseta a posição/preferência. */
    suspend fun clearAll()

    /**
     * Pausa do download automático, gravada por "Apagar Bíblia": enquanto existir, o boot não baixa nada.
     * Sobrevive a [clearAll]; só um download manual a tira.
     */
    suspend fun isAutoDownloadPaused(): Boolean

    suspend fun setAutoDownloadPaused(paused: Boolean)

    /** Bytes que as traduções baixadas ocupam no aparelho. */
    suspend fun diskUsageBytes(): Long
}
