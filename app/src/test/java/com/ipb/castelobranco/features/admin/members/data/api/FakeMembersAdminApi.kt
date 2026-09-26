package com.ipb.castelobranco.features.admin.members.data.api

import com.ipb.castelobranco.features.admin.members.data.dto.HistoryDto
import com.ipb.castelobranco.features.admin.members.data.dto.MemberListDto
import com.ipb.castelobranco.features.admin.members.data.dto.MemberOptionsDto
import com.ipb.castelobranco.features.admin.members.data.dto.MemberRecordDto
import com.ipb.castelobranco.features.admin.members.data.dto.PhotoUrlDto
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import retrofit2.Response

/**
 * Scripted [MembersAdminApi]. Each call answers from its lambda — which may also throw, e.g. an
 * `IOException` for a dropped connection — and records what it received.
 */
class FakeMembersAdminApi : MembersAdminApi {

    var onGetMembers: (ifNoneMatch: String?) -> Response<MemberListDto> = { unscripted() }
    var onGetOptions: () -> Response<MemberOptionsDto> = { unscripted() }
    var onGetMember: (id: Int, ifNoneMatch: String?) -> Response<MemberRecordDto> = { _, _ -> unscripted() }
    var onCreateMember: (JsonObject) -> Response<MemberRecordDto> = { unscripted() }
    var onUpdateMember: (id: Int, body: JsonObject) -> Response<MemberRecordDto> = { _, _ -> unscripted() }
    var onDeleteMember: (id: Int) -> Response<Unit> = { unscripted() }
    var onUploadPhoto: (id: Int, part: MultipartBody.Part) -> Response<PhotoUrlDto> = { _, _ -> unscripted() }
    var onRemovePhoto: (id: Int) -> Response<Unit> = { unscripted() }
    var onGetHistory: (id: Int) -> Response<HistoryDto> = { unscripted() }

    val membersIfNoneMatch = mutableListOf<String?>()
    val memberIfNoneMatch = mutableListOf<String?>()
    val createdBodies = mutableListOf<JsonObject>()
    val updatedBodies = mutableListOf<Pair<Int, JsonObject>>()
    val uploadedParts = mutableListOf<MultipartBody.Part>()
    var calls = 0
        private set

    override suspend fun getMembers(ifNoneMatch: String?): Response<MemberListDto> {
        calls++
        membersIfNoneMatch += ifNoneMatch
        return onGetMembers(ifNoneMatch)
    }

    override suspend fun getOptions(): Response<MemberOptionsDto> {
        calls++
        return onGetOptions()
    }

    override suspend fun getMember(id: Int, ifNoneMatch: String?): Response<MemberRecordDto> {
        calls++
        memberIfNoneMatch += ifNoneMatch
        return onGetMember(id, ifNoneMatch)
    }

    override suspend fun createMember(body: JsonObject): Response<MemberRecordDto> {
        calls++
        createdBodies += body
        return onCreateMember(body)
    }

    override suspend fun updateMember(id: Int, body: JsonObject): Response<MemberRecordDto> {
        calls++
        updatedBodies += id to body
        return onUpdateMember(id, body)
    }

    override suspend fun deleteMember(id: Int): Response<Unit> {
        calls++
        return onDeleteMember(id)
    }

    override suspend fun uploadPhoto(id: Int, photo: MultipartBody.Part): Response<PhotoUrlDto> {
        calls++
        uploadedParts += photo
        return onUploadPhoto(id, photo)
    }

    override suspend fun removePhoto(id: Int): Response<Unit> {
        calls++
        return onRemovePhoto(id)
    }

    override suspend fun getHistory(id: Int): Response<HistoryDto> {
        calls++
        return onGetHistory(id)
    }

    private fun unscripted(): Nothing = throw AssertionError("Unexpected call to FakeMembersAdminApi")
}
