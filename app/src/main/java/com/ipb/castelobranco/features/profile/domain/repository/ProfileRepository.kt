package com.ipb.castelobranco.features.profile.domain.repository

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.profile.domain.model.MeProfile
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ProfileRepository {

    fun observeMeProfile(): Flow<SnapshotState<MeProfile>>

    suspend fun refreshMeProfile(): RefreshResult

    suspend fun uploadProfilePhoto(
        bytes: ByteArray,
        fileName: String = "profile.jpg"
    ): Result<String?>

    suspend fun deleteProfilePhoto(): Result<Unit>

    suspend fun downloadAndPersistProfilePhoto(photoUrl: String): Result<File?>

    suspend fun clearLocalProfilePhoto(): Result<Unit>

    suspend fun clearProfileSnapshot()

    fun getLocalProfilePhoto(): File?
}