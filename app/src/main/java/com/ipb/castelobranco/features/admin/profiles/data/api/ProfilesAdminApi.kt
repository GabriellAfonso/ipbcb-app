package com.ipb.castelobranco.features.admin.profiles.data.api

import com.ipb.castelobranco.core.network.ApiConstants
import com.ipb.castelobranco.features.admin.profiles.data.dto.ManagedAccountDto
import com.ipb.castelobranco.features.admin.profiles.data.dto.ManagedAccountListDto
import com.ipb.castelobranco.features.admin.profiles.data.dto.DenyRequestBody
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** "Perfis" (backend 022): app accounts as leaders see them. Scope `profiles`; no e-mail anywhere. */
object ProfilesAdminEndpoints {
    private const val ROOT = "${ApiConstants.BASE_PATH}admin/profiles/"

    const val ACCOUNTS_PATH = ROOT
    const val ACCOUNT_PATH = "${ROOT}{id}/"
    const val GRANT_PATH = "${ROOT}{id}/grant-access/"
    const val REVOKE_PATH = "${ROOT}{id}/revoke-access/"
    const val APPROVE_PATH = "${ROOT}requests/{id}/approve/"
    const val DENY_PATH = "${ROOT}requests/{id}/deny/"

    const val ID = "id"
    const val STATUS = "status"
    const val IF_NONE_MATCH = "If-None-Match"
    const val ETAG = "ETag"
}

/** Created from `@AuthedRetrofit`: every endpoint needs a leader. */
interface ProfilesAdminApi {

    @GET(ProfilesAdminEndpoints.ACCOUNTS_PATH)
    suspend fun getAccounts(
        @Query(ProfilesAdminEndpoints.STATUS) status: String,
        @Header(ProfilesAdminEndpoints.IF_NONE_MATCH) ifNoneMatch: String? = null,
    ): Response<ManagedAccountListDto>

    @GET(ProfilesAdminEndpoints.ACCOUNT_PATH)
    suspend fun getAccount(@Path(ProfilesAdminEndpoints.ID) id: String): Response<ManagedAccountDto>

    /** A [JsonObject] so `member_id: null` reaches the wire (the shared `Json` drops nulls). */
    @PATCH(ProfilesAdminEndpoints.ACCOUNT_PATH)
    suspend fun updateAccount(
        @Path(ProfilesAdminEndpoints.ID) id: String,
        @Body body: JsonObject,
    ): Response<ManagedAccountDto>

    @POST(ProfilesAdminEndpoints.GRANT_PATH)
    suspend fun grantAccess(@Path(ProfilesAdminEndpoints.ID) id: String): Response<ManagedAccountDto>

    @POST(ProfilesAdminEndpoints.REVOKE_PATH)
    suspend fun revokeAccess(@Path(ProfilesAdminEndpoints.ID) id: String): Response<ManagedAccountDto>

    @POST(ProfilesAdminEndpoints.APPROVE_PATH)
    suspend fun approveRequest(@Path(ProfilesAdminEndpoints.ID) requestId: Long): Response<ManagedAccountDto>

    @POST(ProfilesAdminEndpoints.DENY_PATH)
    suspend fun denyRequest(
        @Path(ProfilesAdminEndpoints.ID) requestId: Long,
        @Body body: DenyRequestBody,
    ): Response<ManagedAccountDto>
}
