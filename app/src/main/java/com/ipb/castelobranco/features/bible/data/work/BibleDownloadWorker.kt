package com.ipb.castelobranco.features.bible.data.work

import android.content.Context
import timber.log.Timber
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ipb.castelobranco.features.bible.data.api.BibleApi
import com.ipb.castelobranco.features.bible.data.local.BibleLocalStore
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.ipb.castelobranco.core.network.error.parseApiError
import org.json.JSONObject

/**
 * Baixa o JSON de cada [BibleTranslation] e o divide por livro via [BibleLocalStore].
 *
 * Skip por tradução: se a tradução já está no disco no formato atual, não rebaixa (a menos que [INPUT_FORCE] esteja
 * true). Uma no formato antigo (sem títulos) é baixada de novo.
 *
 * Importante: a resposta é lida em fluxo, um livro por vez — nunca a tradução inteira em memória. Cada tradução
 * tem ~4 MB e ~31 mil strings; decodificar tudo de uma vez causava OOM e GC longa na emulação, fazendo o worker
 * travar e voltar como RETRY indefinidamente.
 *
 * Progresso é reportado em incrementos por tradução (0 / N → 1 / N → 2 / N).
 */
@HiltWorker
class BibleDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: BibleApi,
    private val store: BibleLocalStore,
    private val repository: BibleRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val force = inputData.getBoolean(INPUT_FORCE, false)
            val translations = BibleTranslation.entries
            val total = translations.size
            var done = 0

            // Converts an old single-file translation instead of downloading it again.
            repository.preload()

            setProgressAsync(workDataOf(KEY_DOWNLOADED to 0, KEY_TOTAL to total))

            for (translation in translations) {
                val alreadyCached = !force && store.isCurrent(translation)
                if (!alreadyCached) {
                    val response = api.getBibleRaw(translation.code, ifNoneMatch = null)
                    if (!response.isSuccessful) {
                        val code = response.code()
                        val raw = response.errorBody()?.string()
                        val parsed = parseApiError(raw)
                        val errorMessage = parsed?.detail?.ifBlank { null }
                            ?: raw?.let {
                                try {
                                    JSONObject(it).optString("detail", "").ifBlank { null }
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            ?: raw?.ifBlank { null }
                            ?: "HTTP $code"
                        if (code == 401 || code == 403) {
                            return Result.failure(workDataOf(KEY_ERROR to errorMessage, KEY_ERROR_CODE to code))
                        }
                        return if (runAttemptCount < MAX_RETRIES) Result.retry()
                        else Result.failure(workDataOf(KEY_ERROR to errorMessage, KEY_ERROR_CODE to code))
                    }
                    val body = response.body() ?: return Result.failure(
                        workDataOf(KEY_ERROR to "Resposta vazia", KEY_ERROR_CODE to response.code())
                    )
                    body.use { store.import(translation, it.byteStream()) }
                }
                done++
                setProgressAsync(workDataOf(KEY_DOWNLOADED to done, KEY_TOTAL to total))
            }

            // Recarrega flow do repositório para que a UI veja os livros sem reabrir o app.
            repository.preload()

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Bible download failed (attempt=%d)", runAttemptCount)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME      = "bible_auto_download"
        const val KEY_DOWNLOADED = "downloaded"
        const val KEY_TOTAL      = "total"
        const val KEY_ERROR      = "error"
        const val KEY_ERROR_CODE = "error_code"
        const val INPUT_FORCE    = "force"

        /** Marks the silent re-download of old-layout translations: the reader shows no banner for it. */
        const val TAG_FORMAT_UPGRADE = "bible_format_upgrade"
        private const val MAX_RETRIES = 3
    }
}
