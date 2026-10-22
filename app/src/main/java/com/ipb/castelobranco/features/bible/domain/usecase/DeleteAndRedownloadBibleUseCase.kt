package com.ipb.castelobranco.features.bible.domain.usecase

import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import javax.inject.Inject

class DeleteAndRedownloadBibleUseCase @Inject constructor(
    private val repository: BibleRepository,
    private val autoDownload: BibleAutoDownloadUseCase,
) {
    suspend operator fun invoke() {
        repository.clearAll()
        autoDownload.enqueueAnyNetwork(force = true)
    }
}
