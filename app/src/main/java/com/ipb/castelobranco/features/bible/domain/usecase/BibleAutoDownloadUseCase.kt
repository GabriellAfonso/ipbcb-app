package com.ipb.castelobranco.features.bible.domain.usecase

import com.ipb.castelobranco.features.bible.domain.download.BibleDownloadScheduler
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import javax.inject.Inject

class BibleAutoDownloadUseCase @Inject constructor(
    private val scheduler: BibleDownloadScheduler,
    private val repository: BibleRepository,
) {
    /**
     * Enfileira o download (WiFi only) se ainda falta alguma tradução em cache e o download automático
     * não foi pausado por "Apagar Bíblia". Sem nada faltando, mas com tradução no formato antigo (sem títulos),
     * enfileira a atualização silenciosa.
     * Deve rodar APÓS [BibleRepository.preload] para que `cachedTranslationsFlow` reflita o disco.
     */
    suspend fun triggerIfNeeded() {
        if (repository.isAutoDownloadPaused()) return
        val cached = repository.cachedTranslationsFlow.value
        val missing = BibleTranslation.entries.any { it !in cached }
        when {
            // The regular download also refreshes old-layout translations.
            missing -> scheduler.enqueueWifiOnly()
            repository.outdatedTranslationsFlow.value.isNotEmpty() -> scheduler.enqueueFormatUpgrade()
        }
    }

    /** Download manual (botões do leitor): tira a pausa deixada por "Apagar Bíblia". */
    suspend fun enqueueWifiOnly(replaceExisting: Boolean = false, force: Boolean = false) {
        repository.setAutoDownloadPaused(false)
        scheduler.enqueueWifiOnly(replaceExisting, force)
    }

    /** Download manual com qualquer rede: tira a pausa deixada por "Apagar Bíblia". */
    suspend fun enqueueAnyNetwork(force: Boolean = false) {
        repository.setAutoDownloadPaused(false)
        scheduler.enqueueAnyNetwork(force)
    }
}
