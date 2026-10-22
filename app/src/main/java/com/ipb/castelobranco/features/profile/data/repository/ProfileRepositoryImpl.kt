package com.ipb.castelobranco.features.profile.data.repository
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.profile.data.photo.ProfilePhotoDataSource
import com.ipb.castelobranco.features.profile.data.snapshot.ProfileSnapshotRepository
import com.ipb.castelobranco.features.profile.domain.model.MeProfile
import com.ipb.castelobranco.features.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val meProfileSnapshot: ProfileSnapshotRepository,
    private val photoDataSource: ProfilePhotoDataSource,
) : ProfileRepository {

    override fun observeMeProfile(): Flow<SnapshotState<MeProfile>> =
        meProfileSnapshot.observe()


    override suspend fun refreshMeProfile(): RefreshResult =
        meProfileSnapshot.refresh()

    override suspend fun uploadProfilePhoto(bytes: ByteArray, fileName: String): Result<String?> =
        photoDataSource.upload(bytes = bytes, fileName = fileName)

    override suspend fun deleteProfilePhoto(): Result<Unit> =
        photoDataSource.delete()

    override suspend fun downloadAndPersistProfilePhoto(photoUrl: String): Result<File?> =
        photoDataSource.downloadAndPersist(photoUrl = photoUrl)

    override suspend fun clearLocalProfilePhoto(): Result<Unit> =
        photoDataSource.clearLocal()

    override suspend fun clearProfileSnapshot() =
        meProfileSnapshot.clearCache()

    override fun getLocalProfilePhoto(): File? =
        photoDataSource.findLastLocalPhotoOrNull()
}