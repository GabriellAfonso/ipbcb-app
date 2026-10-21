package com.ipb.castelobranco.features.profile.data.api

import com.ipb.castelobranco.features.profile.data.dto.ChangePasswordRequest
import com.ipb.castelobranco.features.profile.data.dto.DeleteAccountWithGoogleRequest
import com.ipb.castelobranco.features.profile.data.dto.DeleteAccountWithPasswordRequest
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import com.ipb.castelobranco.features.profile.data.dto.ProfilePhotoResponseDto
import com.ipb.castelobranco.features.profile.data.dto.SessionTokensDto
import com.ipb.castelobranco.features.profile.data.dto.UpdateProfileRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ProfileApi {

    @Multipart
    @POST(ProfileEndpoints.ME_PROFILE_PHOTO_PATH)
    suspend fun uploadProfilePhoto(
        @Part photo: MultipartBody.Part
    ): Response<ProfilePhotoResponseDto>

    @DELETE(ProfileEndpoints.ME_PROFILE_PHOTO_PATH)
    suspend fun deleteProfilePhoto(): Response<Unit>

    @GET(ProfileEndpoints.ME_PROFILE_PATH)
    suspend fun getMeProfile(
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<MeProfileDto>

    @PATCH(ProfileEndpoints.ME_PROFILE_PATH)
    suspend fun updateMeProfile(@Body body: UpdateProfileRequest): Response<MeProfileDto>

    @POST(ProfileEndpoints.ME_PASSWORD_PATH)
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<SessionTokensDto>

    @POST(ProfileEndpoints.ME_DELETE_PATH)
    suspend fun deleteAccountWithPassword(@Body body: DeleteAccountWithPasswordRequest): Response<Unit>

    @POST(ProfileEndpoints.ME_DELETE_PATH)
    suspend fun deleteAccountWithGoogle(@Body body: DeleteAccountWithGoogleRequest): Response<Unit>

    @Streaming
    @GET
    suspend fun downloadFile(
        @Url absoluteUrl: String,
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<ResponseBody>
}