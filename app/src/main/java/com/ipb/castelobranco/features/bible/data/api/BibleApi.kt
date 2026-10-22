package com.ipb.castelobranco.features.bible.data.api

import com.ipb.castelobranco.features.bible.data.dto.BibleBookDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface BibleApi {
    @GET(BibleEndpoints.BIBLE_PATH)
    suspend fun getBible(
        @Path("translation") translation: String,
        @Header("If-None-Match") ifNoneMatch: String?,
    ): Response<List<BibleBookDto>>

    /**
     * Versão "raw" usada pelo download em background: retorna o JSON bruto
     * sem parse/re-encode. Evita OOM e GCs longas ao gravar ~4 MB no disco.
     */
    @GET(BibleEndpoints.BIBLE_PATH)
    suspend fun getBibleRaw(
        @Path("translation") translation: String,
        @Header("If-None-Match") ifNoneMatch: String?,
    ): Response<ResponseBody>
}
