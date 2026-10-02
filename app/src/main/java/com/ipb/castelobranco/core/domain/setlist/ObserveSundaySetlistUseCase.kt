package com.ipb.castelobranco.core.domain.setlist

import com.ipb.castelobranco.core.domain.util.DateProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The Sunday setlist to show: the stored one until the end of its Sunday, `null` afterwards — so the
 * section disappears by itself even when nothing is fetched again.
 */
class ObserveSundaySetlistUseCase @Inject constructor(
    private val repository: SundaySetlistRepository,
    private val dateProvider: DateProvider,
) {
    operator fun invoke(): Flow<SundaySetlist?> = repository.stored.map { setlist ->
        setlist?.takeIf { !dateProvider.today().isAfter(it.date) }
    }
}
