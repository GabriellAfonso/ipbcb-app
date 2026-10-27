package com.ipb.castelobranco.features.bible.data.catalog

import com.ipb.castelobranco.core.domain.bible.BibleBookCatalog
import com.ipb.castelobranco.core.domain.bible.BibleBookInfo
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BibleBookCatalogImpl @Inject constructor(
    private val repository: BibleRepository,
) : BibleBookCatalog {

    override val books: Flow<List<BibleBookInfo>> = repository.booksFlow.map { books ->
        books.orEmpty().map { BibleBookInfo(abbrev = it.displayAbbrev, name = it.name, verseCounts = it.verseCounts) }
    }

    override suspend fun load() {
        if (repository.booksFlow.value == null) repository.preload()
    }
}
