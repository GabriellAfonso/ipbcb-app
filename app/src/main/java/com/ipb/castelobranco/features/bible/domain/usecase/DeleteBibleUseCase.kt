package com.ipb.castelobranco.features.bible.domain.usecase

import com.ipb.castelobranco.features.bible.domain.download.BibleDownloadScheduler
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import javax.inject.Inject

/**
 * "Apagar Bíblia": deletes both translations and pauses the automatic download, so nothing comes
 * back until the user taps a download button in the reader.
 */
class DeleteBibleUseCase @Inject constructor(
    private val repository: BibleRepository,
    private val scheduler: BibleDownloadScheduler,
) {
    suspend operator fun invoke() {
        // A queued download would bring the Bible right back.
        scheduler.cancel()
        repository.setAutoDownloadPaused(true)
        repository.clearAll()
    }
}
