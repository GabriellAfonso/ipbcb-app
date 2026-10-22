package com.ipb.castelobranco.features.bible.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Streaming

interface BibleApi {
    /**
     * JSON bruto da tradução inteira (~4 MB), lido em fluxo pelo download em background: o worker divide um
     * livro por vez em vez de decodificar a resposta inteira.
     */
    @Streaming
    @GET(BibleEndpoints.BIBLE_PATH)
    suspend fun getBibleRaw(
        @Path("translation") translation: String,
        @Header("If-None-Match") ifNoneMatch: String?,
    ): Response<ResponseBody>
}
