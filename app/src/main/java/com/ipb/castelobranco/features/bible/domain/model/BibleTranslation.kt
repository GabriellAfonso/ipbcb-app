package com.ipb.castelobranco.features.bible.domain.model

/**
 * Traduções da Bíblia disponíveis para download.
 *
 * - [code]: identificador usado na URL da API ("NAA", "ARA").
 * - [snapshotKey]: chave usada pelo SnapshotStorage / DataStore.
 */
enum class BibleTranslation(val code: String, val snapshotKey: String, val displayName: String) {
    NAA(code = "NAA", snapshotKey = "bible_naa", displayName = "NAA"),
    ARA(code = "ARA", snapshotKey = "bible_ara", displayName = "ARA");

    companion object {
        val Default: BibleTranslation = NAA

        fun fromCode(code: String?): BibleTranslation =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: Default
    }
}
