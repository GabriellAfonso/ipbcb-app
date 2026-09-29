package com.ipb.castelobranco.features.profile.data.repository

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.profile.data.photo.ProfilePhotoDataSource
import com.ipb.castelobranco.features.profile.data.snapshot.ProfileSnapshotRepository
import com.ipb.castelobranco.features.profile.domain.model.MeProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryImplTest {

    private lateinit var snapshotRepository: ProfileSnapshotRepository
    private lateinit var photoDataSource: ProfilePhotoDataSource
    private lateinit var repository: ProfileRepositoryImpl

    private val fakeProfile = MeProfile(
        name = "João Silva",
        isMember = true,
        photoUrl = "https://example.com/photo.jpg"
    )

    @Before
    fun setup() {
        snapshotRepository = mockk()
        photoDataSource = mockk()
        repository = ProfileRepositoryImpl(snapshotRepository, photoDataSource)
    }

    // region observeMeProfile

    @Test
    fun `observeMeProfile returns flow from snapshotRepository observe`() = runTest {
        val stateFlow = MutableStateFlow<SnapshotState<MeProfile>>(SnapshotState.Data(fakeProfile))
        every { snapshotRepository.observe() } returns stateFlow

        val result = repository.observeMeProfile()

        assertEquals(stateFlow, result)
    }

    // endregion

    // region refreshMeProfile

    @Test
    fun `refreshMeProfile delegates to snapshotRepository refresh`() = runTest {
        coEvery { snapshotRepository.refresh() } returns RefreshResult.Updated

        repository.refreshMeProfile()

        coVerify { snapshotRepository.refresh() }
    }

    @Test
    fun `refreshMeProfile returns Updated on success`() = runTest {
        coEvery { snapshotRepository.refresh() } returns RefreshResult.Updated

        val result = repository.refreshMeProfile()

        assertEquals(RefreshResult.Updated, result)
    }

    @Test
    fun `refreshMeProfile returns Error when snapshot returns error`() = runTest {
        val error = RefreshResult.Error(RuntimeException("network failure"))
        coEvery { snapshotRepository.refresh() } returns error

        val result = repository.refreshMeProfile()

        assertTrue(result is RefreshResult.Error)
    }

    // endregion

    // region uploadProfilePhoto

    @Test
    fun `uploadProfilePhoto delegates to photoDataSource upload`() = runTest {
        val bytes = ByteArray(10)
        coEvery { photoDataSource.upload(bytes, "photo.jpg") } returns Result.success("https://example.com/new.jpg")

        repository.uploadProfilePhoto(bytes, "photo.jpg")

        coVerify { photoDataSource.upload(bytes, "photo.jpg") }
    }

    @Test
    fun `uploadProfilePhoto returns success result from photoDataSource`() = runTest {
        val bytes = ByteArray(10)
        coEvery { photoDataSource.upload(bytes, "photo.jpg") } returns Result.success("url")

        val result = repository.uploadProfilePhoto(bytes, "photo.jpg")

        assertTrue(result.isSuccess)
        assertEquals("url", result.getOrNull())
    }

    @Test
    fun `uploadProfilePhoto returns failure when photoDataSource fails`() = runTest {
        val bytes = ByteArray(10)
        coEvery { photoDataSource.upload(bytes, "photo.jpg") } returns Result.failure(RuntimeException("upload failed"))

        val result = repository.uploadProfilePhoto(bytes, "photo.jpg")

        assertTrue(result.isFailure)
    }

    // endregion

    // region deleteProfilePhoto

    @Test
    fun `deleteProfilePhoto delegates to photoDataSource delete`() = runTest {
        coEvery { photoDataSource.delete() } returns Result.success(Unit)

        repository.deleteProfilePhoto()

        coVerify { photoDataSource.delete() }
    }

    @Test
    fun `deleteProfilePhoto returns success from photoDataSource`() = runTest {
        coEvery { photoDataSource.delete() } returns Result.success(Unit)

        val result = repository.deleteProfilePhoto()

        assertTrue(result.isSuccess)
    }

    // endregion

    // region downloadAndPersistProfilePhoto

    @Test
    fun `downloadAndPersistProfilePhoto delegates to photoDataSource downloadAndPersist`() = runTest {
        val url = "https://example.com/photo.jpg"
        val file = mockk<File>()
        coEvery { photoDataSource.downloadAndPersist(url) } returns Result.success(file)

        repository.downloadAndPersistProfilePhoto(url)

        coVerify { photoDataSource.downloadAndPersist(url) }
    }

    @Test
    fun `downloadAndPersistProfilePhoto returns the file from photoDataSource`() = runTest {
        val url = "https://example.com/photo.jpg"
        val file = mockk<File>()
        coEvery { photoDataSource.downloadAndPersist(url) } returns Result.success(file)

        val result = repository.downloadAndPersistProfilePhoto(url)

        assertEquals(file, result.getOrNull())
    }

    // endregion

    // region clearLocalProfilePhoto

    @Test
    fun `clearLocalProfilePhoto delegates to photoDataSource clearLocal`() = runTest {
        coEvery { photoDataSource.clearLocal() } returns Result.success(Unit)

        repository.clearLocalProfilePhoto()

        coVerify { photoDataSource.clearLocal() }
    }

    // endregion

    // region getLocalProfilePhoto

    @Test
    fun `getLocalProfilePhoto delegates to photoDataSource findLastLocalPhotoOrNull`() {
        val file = mockk<File>()
        every { photoDataSource.findLastLocalPhotoOrNull() } returns file

        val result = repository.getLocalProfilePhoto()

        verify { photoDataSource.findLastLocalPhotoOrNull() }
        assertEquals(file, result)
    }

    @Test
    fun `getLocalProfilePhoto returns null when photoDataSource returns null`() {
        every { photoDataSource.findLastLocalPhotoOrNull() } returns null

        val result = repository.getLocalProfilePhoto()

        assertNull(result)
    }

    // endregion
}
