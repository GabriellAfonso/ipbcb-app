package com.ipb.castelobranco.features.admin.members.data.api

import com.ipb.castelobranco.features.admin.members.data.dto.HistoryDto
import com.ipb.castelobranco.features.admin.members.data.dto.MemberListDto
import com.ipb.castelobranco.features.admin.members.data.dto.MemberOptionsDto
import com.ipb.castelobranco.features.admin.members.data.dto.MemberRecordDto
import com.ipb.castelobranco.features.admin.members.data.dto.PhotoUrlDto
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Every call here requires a leader, so the interface is created from `@AuthedRetrofit` — the
 * wrong qualifier would be a silent 401 on every screen of the area.
 *
 * Writes take a [JsonObject] instead of a DTO: the shared `Json` drops null properties
 * (`explicitNulls = false`), and a PATCH needs `null` on the wire to clear a field.
 */
interface MembersAdminApi {

    @GET(MembersAdminEndpoints.MEMBERS_PATH)
    suspend fun getMembers(
        @Header(MembersAdminEndpoints.IF_NONE_MATCH) ifNoneMatch: String? = null,
    ): Response<MemberListDto>

    @GET(MembersAdminEndpoints.OPTIONS_PATH)
    suspend fun getOptions(): Response<MemberOptionsDto>

    @GET(MembersAdminEndpoints.MEMBER_PATH)
    suspend fun getMember(
        @Path(MembersAdminEndpoints.ID) id: Int,
        @Header(MembersAdminEndpoints.IF_NONE_MATCH) ifNoneMatch: String? = null,
    ): Response<MemberRecordDto>

    @POST(MembersAdminEndpoints.MEMBERS_PATH)
    suspend fun createMember(@Body body: JsonObject): Response<MemberRecordDto>

    @PATCH(MembersAdminEndpoints.MEMBER_PATH)
    suspend fun updateMember(
        @Path(MembersAdminEndpoints.ID) id: Int,
        @Body body: JsonObject,
    ): Response<MemberRecordDto>

    @DELETE(MembersAdminEndpoints.MEMBER_PATH)
    suspend fun deleteMember(@Path(MembersAdminEndpoints.ID) id: Int): Response<Unit>

    @Multipart
    @PUT(MembersAdminEndpoints.PHOTO_PATH)
    suspend fun uploadPhoto(
        @Path(MembersAdminEndpoints.ID) id: Int,
        @Part photo: MultipartBody.Part,
    ): Response<PhotoUrlDto>

    @DELETE(MembersAdminEndpoints.PHOTO_PATH)
    suspend fun removePhoto(@Path(MembersAdminEndpoints.ID) id: Int): Response<Unit>

    @GET(MembersAdminEndpoints.HISTORY_PATH)
    suspend fun getHistory(@Path(MembersAdminEndpoints.ID) id: Int): Response<HistoryDto>
}
