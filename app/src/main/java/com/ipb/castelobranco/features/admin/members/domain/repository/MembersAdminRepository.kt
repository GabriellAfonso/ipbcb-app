package com.ipb.castelobranco.features.admin.members.domain.repository

import com.ipb.castelobranco.features.admin.members.domain.model.HistoryEntry
import com.ipb.castelobranco.features.admin.members.domain.model.MemberChanges
import com.ipb.castelobranco.features.admin.members.domain.model.MemberOptions
import com.ipb.castelobranco.features.admin.members.domain.model.MemberRecord
import com.ipb.castelobranco.features.admin.members.domain.model.MemberSummary
import kotlinx.coroutines.flow.Flow

/**
 * The church roll as a leader sees it. Online only and memory only: nothing here is ever written
 * to the device (LGPD art. 11). Every write updates the in-memory list, so the list and the
 * profile stay in step without the screens talking to each other.
 *
 * Failures are always `AppError`. A 404 on a member carries the user message
 * "Este membro não existe mais" and drops that member from the list.
 */
interface MembersAdminRepository {

    /** Null until the list is loaded in this session. */
    fun observeMembers(): Flow<List<MemberSummary>?>

    suspend fun refreshMembers(): Result<Unit>

    suspend fun getMember(id: Int): Result<MemberRecord>

    suspend fun getOptions(): Result<MemberOptions>

    suspend fun createMember(changes: MemberChanges): Result<MemberRecord>

    suspend fun updateMember(id: Int, changes: MemberChanges): Result<MemberRecord>

    suspend fun deleteMember(id: Int): Result<Unit>

    /** @return the new photo URL */
    suspend fun uploadPhoto(id: Int, bytes: ByteArray, mimeType: String): Result<String>

    suspend fun removePhoto(id: Int): Result<Unit>

    /** Newest first, as the server sends it. */
    suspend fun getHistory(id: Int): Result<List<HistoryEntry>>

    /** Forgets everything held for this session. */
    suspend fun clear()
}
