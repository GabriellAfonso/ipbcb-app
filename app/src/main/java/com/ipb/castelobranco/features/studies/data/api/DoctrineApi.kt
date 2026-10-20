package com.ipb.castelobranco.features.studies.data.api

import com.ipb.castelobranco.features.studies.data.dto.DoctrineDocumentDto
import com.ipb.castelobranco.features.studies.data.dto.DoctrineListDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/** Public, read-only; both endpoints answer 304 to a matching `If-None-Match`. */
interface DoctrineApi {
    @GET(DoctrineEndpoints.DOCUMENTS_PATH)
    suspend fun getDocuments(
        @Header("If-None-Match") ifNoneMatch: String?,
    ): Response<DoctrineListDto>

    @GET(DoctrineEndpoints.DOCUMENT_PATH)
    suspend fun getDocument(
        @Path("slug") slug: String,
        @Header("If-None-Match") ifNoneMatch: String?,
    ): Response<DoctrineDocumentDto>
}
