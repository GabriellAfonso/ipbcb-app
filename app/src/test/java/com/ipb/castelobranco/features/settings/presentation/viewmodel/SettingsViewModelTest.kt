package com.ipb.castelobranco.features.settings.presentation.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import com.ipb.castelobranco.features.bible.domain.usecase.DeleteBibleUseCase
import com.ipb.castelobranco.core.domain.gallery.GalleryStoragePort
import com.ipb.castelobranco.core.domain.gallery.GalleryUsage
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
    private lateinit var galleryStorage: GalleryStoragePort
    private lateinit var deleteBible: DeleteBibleUseCase
    private lateinit var bibleRepository: BibleRepository
    private lateinit var viewModel: SettingsViewModel

    private val themeModeFlow = MutableSharedFlow<ThemeMode>(replay = 1)
    private val birthdayNotificationsFlow = MutableStateFlow(true)
    private val keepScreenOnFlow = MutableStateFlow(true)
    private val scheduleNotificationsFlow = MutableStateFlow(true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true)
        galleryStorage = mockk(relaxed = true)
        deleteBible = mockk(relaxed = true)
        bibleRepository = mockk(relaxed = true)
        every { settingsRepository.themeModeFlow } returns themeModeFlow
        every { settingsRepository.birthdayNotificationsFlow } returns birthdayNotificationsFlow
        every { settingsRepository.keepScreenOnFlow } returns keepScreenOnFlow
        every { settingsRepository.scheduleNotificationsFlow } returns scheduleNotificationsFlow
        coEvery { galleryStorage.usage() } returns GALLERY_USAGE
        coEvery { bibleRepository.diskUsageBytes() } returns BIBLE_BYTES
        viewModel = SettingsViewModel(
            settingsRepository,
            galleryStorage,
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
            assertEquals(GALLERY_USAGE, state.galleryUsage)
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
            coEvery { bibleRepository.diskUsageBytes() } returns 0L

            viewModel.requestReset(ResetAction.BIBLE)
            viewModel.confirmReset()
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertTrue(state.bibleCleared)
            assertEquals(0L, state.bibleBytes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirming the Bible delete runs its use case and never touches the gallery`() = runTest {
        viewModel.requestReset(ResetAction.BIBLE)
        viewModel.confirmReset()
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteBible() }
        coVerify(exactly = 0) { galleryStorage.delete(any(), any()) }
    }

    // endregion

    // region diagnostics

    @Test
    fun `diagnostics stay hidden until the version is tapped five times`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            themeModeFlow.emit(ThemeMode.DARK)
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().showDiagnostics)

            repeat(DIAGNOSTICS_UNLOCK_TAPS - 1) { viewModel.onVersionTap() }
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.showDiagnostics)

            viewModel.onVersionTap()
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().showDiagnostics)

            viewModel.onVersionTap()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showDiagnostics)
            cancelAndIgnoreRemainingEvents()
        }
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

    @Test
    fun `the rota reminders switch reflects and saves its value`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            themeModeFlow.emit(ThemeMode.DARK)
            assertEquals(true, awaitItem().scheduleNotifications)

            viewModel.toggleScheduleNotifications()
            advanceUntilIdle()
            coVerify { settingsRepository.setScheduleNotifications(false) }

            scheduleNotificationsFlow.value = false
            assertEquals(false, awaitItem().scheduleNotifications)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    private companion object {
        val GALLERY_USAGE = GalleryUsage(photoBytes = 312L * 1024 * 1024, videoBytes = 0L)
        const val BIBLE_BYTES = 8L * 1024 * 1024
    }
}
