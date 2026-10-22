package com.ipb.castelobranco.features.bible.data.snapshot

import com.ipb.castelobranco.core.data.snapshot.RetrofitSnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.bible.data.api.BibleApi
import com.ipb.castelobranco.features.bible.data.dto.BibleBookDto
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation

/**
 * Um fetcher por tradução. O [translation] é fixado na construção e
 * passado como path param em cada chamada.
 */
class BibleSnapshotFetcher(
    api: BibleApi,
    translation: BibleTranslation,
) : SnapshotFetcher<List<BibleBookDto>>,
    RetrofitSnapshotFetcher<List<BibleBookDto>>(
        call = { etag -> api.getBible(translation.code, etag) }
    )
