package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.profile.domain.model.MeProfile
import com.ipb.castelobranco.features.profile.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class FetchProfileUseCaseTest {

    private lateinit var repository: ProfileRepository
    private lateinit var useCase: FetchProfileUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = FetchProfileUseCase(repository)
    }

    // region getLocalPhoto

    @Test
    fun `getLocalPhoto delegates to repository getLocalProfilePhoto`() {
        val file = mockk<File>()
        every { repository.getLocalProfilePhoto() } returns file

        val result = useCase.getLocalPhoto()

        assertEquals(file, result)
    }

    @Test
    fun `getLocalPhoto returns null when repository returns null`() {
        every { repository.getLocalProfilePhoto() } returns null

        val result = useCase.getLocalPhoto()

        assertNull(result)
    }

    @Test
    fun `getLocalPhoto calls repository exactly once`() {
        every { repository.getLocalProfilePhoto() } returns null

        useCase.getLocalPhoto()

        verify(exactly = 1) { repository.getLocalProfilePhoto() }
    }

    // endregion

    // region observe

    @Test
    fun `observe delegates to repository observeMeProfile`() {
        val flow = flowOf(SnapshotState.Loading)
        every { repository.observeMeProfile() } returns flow

        val result = useCase.observe()

        assertEquals(flow, result)
    }

    // endregion

    // region refresh

    @Test
    fun `refresh delegates to repository refreshMeProfile`() = runTest {
        coEvery { repository.refreshMeProfile() } returns RefreshResult.Updated

        val result = useCase.refresh()

        assertEquals(RefreshResult.Updated, result)
    }

    // endregion

    // endregion

    // region downloadAndPersistPhoto

    @Test
    fun `downloadAndPersistPhoto delegates to repository with correct url`() = runTest {
        val file = mockk<File>()
        coEvery { repository.downloadAndPersistProfilePhoto(any()) } returns Result.success(file)

        val result = useCase.downloadAndPersistPhoto("https://example.com/photo.jpg")

        assertEquals(file, result.getOrNull())
    }

    // endregion

    // region clearLocalPhoto

    @Test
    fun `clearLocalPhoto delegates to repository clearLocalProfilePhoto`() = runTest {
        coEvery { repository.clearLocalProfilePhoto() } returns Result.success(Unit)

        val result = useCase.clearLocalPhoto()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `clearLocalPhoto returns failure when repository fails`() = runTest {
        coEvery { repository.clearLocalProfilePhoto() } returns Result.failure(Exception("IO error"))

        val result = useCase.clearLocalPhoto()

        assertTrue(result.isFailure)
    }

    @Test
    fun `clearLocalPhoto calls repository exactly once`() = runTest {
        coEvery { repository.clearLocalProfilePhoto() } returns Result.success(Unit)

        useCase.clearLocalPhoto()

        coVerify(exactly = 1) { repository.clearLocalProfilePhoto() }
    }

    // endregion
}
