package com.ipb.castelobranco.features.settings.presentation.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import com.ipb.castelobranco.features.bible.domain.usecase.DeleteBibleUseCase
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.usecase.DeleteGalleryUseCase
import com.ipb.castelobranco.features.settings.domain.model.ThemeMode
import com.ipb.castelobranco.features.settings.domain.repository.SettingsRepository
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var galleryRepository: GalleryRepository
    private lateinit var deleteGallery: DeleteGalleryUseCase
    private lateinit var deleteBible: DeleteBibleUseCase
    private lateinit var bibleRepository: BibleRepository
    private lateinit var viewModel: SettingsViewModel

    private val themeModeFlow = MutableSharedFlow<ThemeMode>(replay = 1)
    private val birthdayNotificationsFlow = MutableStateFlow(true)
    private val keepScreenOnFlow = MutableStateFlow(true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true)
        galleryRepository = mockk(relaxed = true)
        deleteGallery = mockk(relaxed = true)
        deleteBible = mockk(relaxed = true)
        bibleRepository = mockk(relaxed = true)
        every { settingsRepository.themeModeFlow } returns themeModeFlow
        every { settingsRepository.birthdayNotificationsFlow } returns birthdayNotificationsFlow
        every { settingsRepository.keepScreenOnFlow } returns keepScreenOnFlow
        coEvery { galleryRepository.diskUsageBytes() } returns GALLERY_BYTES
        coEvery { bibleRepository.diskUsageBytes() } returns BIBLE_BYTES
        viewModel = SettingsViewModel(
            settingsRepository,
            galleryRepository,
            deleteGallery,
            deleteBible,
            bibleRepository,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState initial value is default SettingsUiState before flow emits`() {
        assertEquals(SettingsUiState(), viewModel.uiState.value)
    }

    @Test
    fun `uiState reflects DARK when themeModeFlow emits DARK`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial SettingsUiState()
            themeModeFlow.emit(ThemeMode.DARK)
            val state = awaitItem()
            assertEquals(ThemeMode.DARK, state.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState reflects LIGHT when themeModeFlow emits LIGHT`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial SettingsUiState()
            themeModeFlow.emit(ThemeMode.LIGHT)
            val state = awaitItem()
            assertEquals(ThemeMode.LIGHT, state.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState reflects FOLLOW_SYSTEM when themeModeFlow emits FOLLOW_SYSTEM`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial SettingsUiState()
            themeModeFlow.emit(ThemeMode.DARK) // move away from default first
            awaitItem()
            themeModeFlow.emit(ThemeMode.FOLLOW_SYSTEM)
            val state = awaitItem()
            assertEquals(ThemeMode.FOLLOW_SYSTEM, state.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // region selectThemeMode

    @Test
    fun `selectThemeMode saves the new mode, applies it and asks for recreate`() = runTest {
        mockkStatic(AppCompatDelegate::class)
        every { AppCompatDelegate.setDefaultNightMode(any()) } just Runs

        viewModel.events.test {
            viewModel.selectThemeMode(ThemeMode.DARK)
            advanceUntilIdle()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { settingsRepository.setThemeMode(ThemeMode.DARK) }
        verify { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) }
        unmockkStatic(AppCompatDelegate::class)
    }

    @Test
    fun `selectThemeMode FOLLOW_SYSTEM applies the follow-system night mode`() = runTest {
        mockkStatic(AppCompatDelegate::class)
        every { AppCompatDelegate.setDefaultNightMode(any()) } just Runs

        viewModel.uiState.test {
            awaitItem()
            themeModeFlow.emit(ThemeMode.LIGHT)
            awaitItem()

            viewModel.selectThemeMode(ThemeMode.FOLLOW_SYSTEM)
            advanceUntilIdle()

            coVerify { settingsRepository.setThemeMode(ThemeMode.FOLLOW_SYSTEM) }
            verify {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            cancelAndIgnoreRemainingEvents()
        }

        unmockkStatic(AppCompatDelegate::class)
    }

    @Test
    fun `selectThemeMode with the current mode does nothing`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            themeModeFlow.emit(ThemeMode.DARK)
            awaitItem()

            viewModel.selectThemeMode(ThemeMode.DARK)
            advanceUntilIdle()

            coVerify(exactly = 0) { settingsRepository.setThemeMode(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region disk usage

    @Test
    fun `uiState carries the measured disk usage`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            themeModeFlow.emit(ThemeMode.DARK)
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(GALLERY_BYTES, state.galleryBytes)
            assertEquals(BIBLE_BYTES, state.bibleBytes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirming a reset measures the disk usage again`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            themeModeFlow.emit(ThemeMode.DARK)
            advanceUntilIdle()
            coEvery { galleryRepository.diskUsageBytes() } returns 0L

            viewModel.requestReset(ResetAction.GALLERY)
            viewModel.confirmReset()
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertTrue(state.galleryCleared)
            assertEquals(0L, state.galleryBytes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirming each delete runs its use case`() = runTest {
        viewModel.requestReset(ResetAction.GALLERY)
        viewModel.confirmReset()
        advanceUntilIdle()
        viewModel.requestReset(ResetAction.BIBLE)
        viewModel.confirmReset()
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteGallery() }
        coVerify(exactly = 1) { deleteBible() }
        coVerify(exactly = 0) { galleryRepository.clear() }
    }

    // endregion

    // region keepScreenOn

    @Test
    fun `uiState reflects keepScreenOnFlow`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            themeModeFlow.emit(ThemeMode.DARK)
            assertTrue(awaitItem().keepScreenOn)
            keepScreenOnFlow.value = false
            assertFalse(awaitItem().keepScreenOn)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleKeepScreenOn saves the opposite of the current value`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            themeModeFlow.emit(ThemeMode.DARK)
            awaitItem()

            viewModel.toggleKeepScreenOn()
            advanceUntilIdle()

            coVerify { settingsRepository.setKeepScreenOn(false) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    private companion object {
        const val GALLERY_BYTES = 312L * 1024 * 1024
        const val BIBLE_BYTES = 8L * 1024 * 1024
    }
}
