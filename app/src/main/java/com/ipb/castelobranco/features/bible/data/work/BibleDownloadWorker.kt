package com.ipb.castelobranco.features.bible.data.work

import android.content.Context
import timber.log.Timber
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ipb.castelobranco.core.data.local.SnapshotStorage
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.features.bible.data.api.BibleApi
import com.ipb.castelobranco.features.bible.data.dto.BibleBookDto
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.ipb.castelobranco.core.network.error.parseApiError
import org.json.JSONObject

/**
 * Baixa o JSON de cada [BibleTranslation] e persiste via [SnapshotStorage].
 *
 * Skip por tradução: se já existe arquivo para a chave, não rebaixa
 * (a menos que [INPUT_FORCE] esteja true — usado pelo botão "Apagar Bíblia").
 *
 * Importante: o worker grava o JSON **bruto** retornado pelo servidor, sem
 * fazer parse → re-encode. Cada tradução tem ~4 MB e ~31 mil strings; o
 * round-trip via kotlinx.serialization causava OOM e GC longa na emulação,
 * fazendo o worker travar e voltar como RETRY indefinidamente.
 *
 * Progresso é reportado em incrementos por tradução (0 / N → 1 / N → 2 / N).
 */
@HiltWorker
class BibleDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: BibleApi,
    private val caches: Map<BibleTranslation, @JvmSuppressWildcards SnapshotCache<List<BibleBookDto>>>,
    private val storage: SnapshotStorage,
    private val repository: BibleRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val force = inputData.getBoolean(INPUT_FORCE, false)
            val translations = BibleTranslation.entries
            val total = translations.size
            var done = 0

            setProgressAsync(workDataOf(KEY_DOWNLOADED to 0, KEY_TOTAL to total))

            for (translation in translations) {
                val cache = caches[translation] ?: continue
                val alreadyCached = !force && cache.exists()
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
                    // Lê e grava como string crua, sem parse/re-encode.
                    val rawJson = body.use { it.string() }
                    val etag = response.headers()["ETag"]
                    storage.save(translation.snapshotKey, rawJson)
                    if (etag != null) storage.saveETag(translation.snapshotKey, etag)
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
        private const val MAX_RETRIES = 3
    }
}
