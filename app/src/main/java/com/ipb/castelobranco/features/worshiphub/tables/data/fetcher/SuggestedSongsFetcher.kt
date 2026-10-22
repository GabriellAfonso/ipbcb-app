package com.ipb.castelobranco.features.worshiphub.tables.data.fetcher

import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.features.worshiphub.tables.data.api.SongsTableApi
import com.ipb.castelobranco.features.worshiphub.tables.data.api.SuggestedSongsExcludeEncoder
import com.ipb.castelobranco.features.worshiphub.tables.data.api.SuggestedSongsFixedEncoder
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.SuggestedSongDto
import timber.log.Timber
import javax.inject.Inject

class SuggestedSongsFetcher @Inject constructor(
    private val api: SongsTableApi
) {
    suspend fun fetch(
        fixedByPosition: Map<Int, Int>,
        excludedByPosition: Map<Int, Set<Int>>,
    ): NetworkResult<List<SuggestedSongDto>> {
        return try {
            val response = api.getSuggestedSongs(
                ifNoneMatch = null,
                fixed = SuggestedSongsFixedEncoder.encode(fixedByPosition),
                exclude = SuggestedSongsExcludeEncoder.encode(excludedByPosition),
            )

            if (!response.isSuccessful) {
                NetworkResult.Failure(IllegalStateException("HTTP ${response.code()}"))
            } else {
                val body = response.body()
                if (body == null) {
                    NetworkResult.Failure(IllegalStateException("Resposta vazia"))
                } else {
                    NetworkResult.Success(
                        body = body,
                        etag = response.headers()["ETag"]?.trim()
                    )
                }
            }
        } catch (t: Throwable) {
            Timber.w(t, "Failed to fetch suggested songs")
            NetworkResult.Failure(t)
        }
    }
}