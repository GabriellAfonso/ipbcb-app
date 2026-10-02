package com.ipb.castelobranco.core.presentation.viewmodel

import com.ipb.castelobranco.core.domain.access.Access
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.RefreshAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.auth.AuthEventBus
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.core.testing.FakeDevicesRepository
import com.ipb.castelobranco.core.testing.FakePushRegistrationScheduler
import com.ipb.castelobranco.core.testing.FakeRegisteredTokenStore
import com.ipb.castelobranco.core.data.app.AppForegroundState
import com.ipb.castelobranco.core.domain.push.UnregisterDeviceUseCase
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.FakeSundaySetlistRepository
import com.ipb.castelobranco.core.testing.FakeWorshipAccessRepository
import com.ipb.castelobranco.core.testing.WORSHIP_MEMBER
import com.ipb.castelobranco.core.testing.setlistOf
import com.ipb.castelobranco.core.domain.auth.SessionPresenceProvider
import com.ipb.castelobranco.core.domain.setlist.SyncSundaySetlistUseCase
import com.ipb.castelobranco.core.domain.worship.ObserveWorshipAccessUseCase
import com.ipb.castelobranco.core.domain.worship.WorshipAccess
import org.junit.Assert.assertNull
import java.time.LocalDate
import com.ipb.castelobranco.core.domain.session.SessionScopedCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.usecase.PreloadDataUseCase
import com.ipb.castelobranco.features.auth.data.local.AuthSession
import com.ipb.castelobranco.features.auth.domain.usecase.LogoutUseCase
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import com.ipb.castelobranco.features.bible.domain.usecase.BibleAutoDownloadUseCase
import com.ipb.castelobranco.features.gallery.domain.usecase.GalleryAutoDownloadUseCase
import com.ipb.castelobranco.features.profile.domain.model.MeProfile
import com.ipb.castelobranco.features.profile.domain.usecase.FetchProfileUseCase
import com.ipb.castelobranco.features.schedule.domain.repository.ScheduleRepository
import com.ipb.castelobranco.core.domain.repository.MembersRepository
import com.ipb.castelobranco.core.domain.usecase.GetMonthlyBirthdaysUseCase
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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
class CoreViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var preloadDataUseCase: PreloadDataUseCase
    private lateinit var authSession: AuthSession
    private lateinit var fetchProfileUseCase: FetchProfileUseCase
    private lateinit var authEventBus: AuthEventBus
    private lateinit var logoutUseCase: LogoutUseCase
    private lateinit var galleryAutoDownload: GalleryAutoDownloadUseCase
    private lateinit var bibleAutoDownload: BibleAutoDownloadUseCase
    private lateinit var bibleRepository: BibleRepository
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var getMonthlyBirthdaysUseCase: GetMonthlyBirthdaysUseCase
    private lateinit var membersRepository: MembersRepository
    private lateinit var viewModel: CoreViewModel

    private var sessionCacheClears = 0
    private val sessionCache = SessionScopedCache { sessionCacheClears++ }

    private val authEventsFlow = MutableSharedFlow<AuthEventBus.Event>()

    private val accessRepository = FakeAccessRepository()
    private val sundaySetlist = FakeSundaySetlistRepository()
    private val worshipAccess = FakeWorshipAccessRepository()
    private var loggedIn = false
    private val pushScheduler = FakePushRegistrationScheduler()
    private val devices = FakeDevicesRepository()
    private val tokenStore = FakeRegisteredTokenStore("token-1")

    private val fakeProfile = MeProfile(
        name = "João",
        isMember = true,
        photoUrl = null,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        preloadDataUseCase = mockk()
        authSession = mockk()
        fetchProfileUseCase = mockk()
        authEventBus = mockk()
        logoutUseCase = mockk()
        galleryAutoDownload = mockk()
        bibleAutoDownload = mockk()
        bibleRepository = mockk()
        scheduleRepository = mockk()
        getMonthlyBirthdaysUseCase = mockk()
        membersRepository = mockk()

        coEvery { preloadDataUseCase(any()) } just runs
        coEvery { scheduleRepository.clearScheduleCache() } just runs
        every { authSession.isLoggedInFlow } returns emptyFlow()
        coEvery { authSession.isLoggedIn() } returns false
        every { authEventBus.events } returns authEventsFlow
        coEvery { logoutUseCase() } just runs
        coEvery { galleryAutoDownload.onAppForeground() } just runs
        coEvery { galleryAutoDownload.onLoginSuccess() } just runs
        coEvery { galleryAutoDownload.clearOnLogout() } just runs
        every { bibleAutoDownload.triggerIfNeeded() } just runs
        coEvery { bibleRepository.preload() } just runs
        every { getMonthlyBirthdaysUseCase.observe() } returns emptyFlow()
        coEvery { getMonthlyBirthdaysUseCase.refresh() } returns
            com.ipb.castelobranco.core.domain.snapshot.RefreshResult.NotModified
        coEvery { membersRepository.clearBirthdaysCache() } just runs
        coEvery { fetchProfileUseCase.refresh() } returns
            com.ipb.castelobranco.core.domain.snapshot.RefreshResult.NotModified
        every { fetchProfileUseCase.observe() } returns emptyFlow()
        coEvery { fetchProfileUseCase.clearLocalPhoto() } returns Result.success(Unit)
        coEvery { fetchProfileUseCase.clearSnapshot() } just runs

        viewModel = CoreViewModel(
            preloadDataUseCase,
            authSession,
            fetchProfileUseCase,
            authEventBus,
            logoutUseCase,
            galleryAutoDownload,
            bibleAutoDownload,
            bibleRepository,
            scheduleRepository,
            getMonthlyBirthdaysUseCase,
            membersRepository,
            setOf(sessionCache),
            ObserveAccessUseCase(accessRepository),
            RefreshAccessUseCase(accessRepository),
            SyncSundaySetlistUseCase(sundaySetlist, SessionPresenceProvider { loggedIn }, worshipAccess),
            ObserveWorshipAccessUseCase(worshipAccess),
            pushScheduler,
            UnregisterDeviceUseCase(pushScheduler, devices, tokenStore),
            AppForegroundState(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region initialize — isLoggedIn observation

    @Test
    fun `initialize reflects isLoggedInFlow emissions in isLoggedIn state`() = runTest {
        val loginFlow = MutableStateFlow(false)
        every { authSession.isLoggedInFlow } returns loginFlow

        viewModel.initialize()
        advanceUntilIdle()
        assertFalse(viewModel.isLoggedIn.value)

        loginFlow.value = true
        advanceUntilIdle()
        assertTrue(viewModel.isLoggedIn.value)
    }

    // endregion

    // region initialize — app initialization

    @Test
    fun `initialize calls preloadDataUseCase`() = runTest {
        viewModel.initialize()
        advanceUntilIdle()

        coVerify { preloadDataUseCase(any()) }
    }

    @Test
    fun `initialize is idempotent and preloads only once`() = runTest {
        viewModel.initialize()
        viewModel.initialize()
        advanceUntilIdle()

        coVerify(exactly = 1) { preloadDataUseCase(any()) }
    }

    @Test
    fun `onAppForeground syncs the gallery when logged in`() = runTest {
        coEvery { authSession.isLoggedIn() } returns true

        viewModel.onAppForeground()
        advanceUntilIdle()

        coVerify(exactly = 1) { galleryAutoDownload.onAppForeground() }
    }

    @Test
    fun `onAppForeground does not sync the gallery when not logged in`() = runTest {
        coEvery { authSession.isLoggedIn() } returns false

        viewModel.onAppForeground()
        advanceUntilIdle()

        // A galeria é restrita a membros: sem sessão o feed só responderia 401
        coVerify(exactly = 0) { galleryAutoDownload.onAppForeground() }
    }

    @Test
    fun `initialize sets isPreloading to false after app initialization completes`() = runTest {
        viewModel.initialize()
        advanceUntilIdle()

        assertFalse(viewModel.isPreloading.value)
    }

    @Test
    fun `initialize does not call fetchProfileUseCase refresh when not logged in`() = runTest {
        coEvery { authSession.isLoggedIn() } returns false

        viewModel.initialize()
        advanceUntilIdle()

        coVerify(exactly = 0) { fetchProfileUseCase.refresh() }
    }

    @Test
    fun `initialize calls fetchProfileUseCase refresh when logged in`() = runTest {
        coEvery { authSession.isLoggedIn() } returns true
        every { fetchProfileUseCase.observe() } returns flowOf(SnapshotState.Data(fakeProfile))

        viewModel.initialize()
        advanceUntilIdle()

        coVerify { fetchProfileUseCase.refresh() }
    }

    @Test
    fun `initialize downloads photo when profile has photoUrl`() = runTest {
        val profileWithPhoto = fakeProfile.copy(photoUrl = "https://example.com/photo.jpg")
        coEvery { authSession.isLoggedIn() } returns true
        every { fetchProfileUseCase.observe() } returns flowOf(SnapshotState.Data(profileWithPhoto))
        coEvery { fetchProfileUseCase.downloadAndPersistPhoto(any()) } returns Result.success(null)

        viewModel.initialize()
        advanceUntilIdle()

        coVerify { fetchProfileUseCase.downloadAndPersistPhoto("https://example.com/photo.jpg") }
    }

    @Test
    fun `initialize does not download photo when profile photoUrl is null`() = runTest {
        coEvery { authSession.isLoggedIn() } returns true
        every { fetchProfileUseCase.observe() } returns flowOf(SnapshotState.Data(fakeProfile))

        viewModel.initialize()
        advanceUntilIdle()

        coVerify(exactly = 0) { fetchProfileUseCase.downloadAndPersistPhoto(any()) }
    }

    @Test
    fun `initialize does not download photo when profile photoUrl is blank`() = runTest {
        coEvery { authSession.isLoggedIn() } returns true
        every { fetchProfileUseCase.observe() } returns flowOf(
            SnapshotState.Data(fakeProfile.copy(photoUrl = "  "))
        )

        viewModel.initialize()
        advanceUntilIdle()

        coVerify(exactly = 0) { fetchProfileUseCase.downloadAndPersistPhoto(any()) }
    }

    @Test
    fun `initialize swallows exception from profile refresh`() = runTest {
        coEvery { authSession.isLoggedIn() } returns true
        coEvery { fetchProfileUseCase.refresh() } throws RuntimeException("network error")

        viewModel.initialize()
        advanceUntilIdle()

        // ViewModel should not crash; isPreloading ends false
        assertFalse(viewModel.isPreloading.value)
    }

    // endregion

    // region initialize — LoginSuccess event

    @Test
    fun `initialize on LoginSuccess event calls fetchProfileUseCase refresh again`() = runTest {
        coEvery { authSession.isLoggedIn() } returns true
        every { fetchProfileUseCase.observe() } returns flowOf(SnapshotState.Data(fakeProfile))

        viewModel.initialize()
        advanceUntilIdle()
        // refresh was called once during startAppInitialization
        coVerify(exactly = 1) { fetchProfileUseCase.refresh() }

        authEventsFlow.emit(AuthEventBus.Event.LoginSuccess)
        advanceUntilIdle()
        // refresh called a second time triggered by LoginSuccess event
        coVerify(exactly = 2) { fetchProfileUseCase.refresh() }
    }

    @Test
    fun `initialize on LoginSuccess re-enqueues the gallery download`() = runTest {
        viewModel.initialize()
        advanceUntilIdle()

        authEventsFlow.emit(AuthEventBus.Event.LoginSuccess)
        advanceUntilIdle()

        // REPLACE descarta um WorkInfo com 401 registrado enquanto ainda estava deslogado
        coVerify(exactly = 1) { galleryAutoDownload.onLoginSuccess() }
    }

    @Test
    fun `initialize on LoginSuccess does not refresh when not logged in`() = runTest {
        coEvery { authSession.isLoggedIn() } returns false

        viewModel.initialize()
        advanceUntilIdle()

        authEventsFlow.emit(AuthEventBus.Event.LoginSuccess)
        advanceUntilIdle()

        coVerify(exactly = 0) { fetchProfileUseCase.refresh() }
    }

    // endregion

    // region access — panel entry and refresh after 403

    @Test
    fun `canOpenPanel is false without any role`() = runTest {
        advanceUntilIdle()

        assertFalse(viewModel.canOpenPanel.value)
    }

    @Test
    fun `canOpenPanel follows the roles in the profile`() = runTest {
        advanceUntilIdle()

        accessRepository.state.value = accessOf(Role.MEDIA)
        advanceUntilIdle()
        assertTrue(viewModel.canOpenPanel.value)

        accessRepository.state.value = Access.NONE
        advanceUntilIdle()
        assertFalse(viewModel.canOpenPanel.value)
    }

    @Test
    fun `an unknown role alone still opens the panel`() = runTest {
        accessRepository.state.value = Access(roles = emptySet(), hasAnyRole = true, levels = emptyMap())
        advanceUntilIdle()

        assertTrue(viewModel.canOpenPanel.value)
    }

    @Test
    fun `PermissionDenied refreshes access`() = runTest {
        viewModel.initialize()
        advanceUntilIdle()

        authEventsFlow.emit(AuthEventBus.Event.PermissionDenied)
        advanceUntilIdle()

        assertEquals(1, accessRepository.refreshCalls)
    }

    @Test
    fun `LoginSuccess does not go through the access refresh`() = runTest {
        viewModel.initialize()
        advanceUntilIdle()

        authEventsFlow.emit(AuthEventBus.Event.LoginSuccess)
        advanceUntilIdle()

        assertEquals(0, accessRepository.refreshCalls)
    }

    // endregion

    // region refreshLoginState

    @Test
    fun `refreshLoginState updates isLoggedIn from authSession`() = runTest {
        coEvery { authSession.isLoggedIn() } returns true

        viewModel.refreshLoginState()
        advanceUntilIdle()

        assertTrue(viewModel.isLoggedIn.value)
    }

    @Test
    fun `refreshLoginState sets isLoggedIn to false when not logged in`() = runTest {
        coEvery { authSession.isLoggedIn() } returns false

        viewModel.refreshLoginState()
        advanceUntilIdle()

        assertFalse(viewModel.isLoggedIn.value)
    }

    // endregion

    // region logout

    @Test
    fun `logout calls fetchProfileUseCase clearLocalPhoto`() = runTest {
        viewModel.logout()
        advanceUntilIdle()

        coVerify { fetchProfileUseCase.clearLocalPhoto() }
    }

    @Test
    fun `logout calls logoutUseCase`() = runTest {
        viewModel.logout()
        advanceUntilIdle()

        coVerify { logoutUseCase() }
    }

    @Test
    fun `logout emits LogoutSuccess event`() = runTest {
        viewModel.events.test {
            viewModel.logout()
            advanceUntilIdle()
            assertEquals(CoreViewModel.CoreEvent.LogoutSuccess, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `logout clears photo and schedule cache before calling logoutUseCase`() = runTest {
        val order = mutableListOf<String>()
        coEvery { fetchProfileUseCase.clearLocalPhoto() } coAnswers {
            order.add("clearPhoto")
            Result.success(Unit)
        }
        coEvery { scheduleRepository.clearScheduleCache() } coAnswers {
            order.add("clearSchedule")
        }
        coEvery { membersRepository.clearBirthdaysCache() } coAnswers {
            order.add("clearBirthdays")
        }
        coEvery { galleryAutoDownload.clearOnLogout() } coAnswers {
            order.add("clearGallery")
        }
        coEvery { logoutUseCase() } coAnswers {
            order.add("logout")
        }

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(
            listOf("clearPhoto", "clearSchedule", "clearBirthdays", "clearGallery", "logout"),
            order
        )
    }

    // endregion

    // region session-scoped caches

    @Test
    fun `logout clears every session-scoped cache`() = runTest {
        viewModel.logout()
        advanceUntilIdle()

        assertEquals(1, sessionCacheClears)
    }

    @Test
    fun `session dropping from logged in to logged out clears session-scoped caches`() = runTest {
        val loginFlow = MutableStateFlow(true)
        every { authSession.isLoggedInFlow } returns loginFlow

        viewModel.initialize()
        advanceUntilIdle()
        assertEquals(0, sessionCacheClears)

        loginFlow.value = false
        advanceUntilIdle()

        assertEquals(1, sessionCacheClears)
    }

    @Test
    fun `staying logged out never clears session-scoped caches`() = runTest {
        val loginFlow = MutableStateFlow(false)
        every { authSession.isLoggedInFlow } returns loginFlow

        viewModel.initialize()
        advanceUntilIdle()

        assertEquals(0, sessionCacheClears)
    }

    // endregion

    // region Sunday setlist

    @Test
    fun `boot reads the current setlist for a worship member`() = runTest {
        loggedIn = true
        coEvery { authSession.isLoggedIn() } returns true
        worshipAccess.state.value = WORSHIP_MEMBER

        viewModel.initialize()
        advanceUntilIdle()

        assertEquals(1, sundaySetlist.refreshCalls)
    }

    @Test
    fun `returning to the app reads it again`() = runTest {
        loggedIn = true
        coEvery { authSession.isLoggedIn() } returns true
        worshipAccess.state.value = WORSHIP_MEMBER
        viewModel.initialize()
        advanceUntilIdle()

        viewModel.onAppForeground()
        advanceUntilIdle()

        assertEquals(2, sundaySetlist.refreshCalls)
    }

    @Test
    fun `leaving the worship ministry clears the stored setlist`() = runTest {
        loggedIn = true
        coEvery { authSession.isLoggedIn() } returns true
        worshipAccess.state.value = WORSHIP_MEMBER
        sundaySetlist.state.value = setlistOf(LocalDate.of(2026, 10, 4), 12)
        viewModel.initialize()
        advanceUntilIdle()

        worshipAccess.state.value = WorshipAccess.NONE
        advanceUntilIdle()

        assertNull(sundaySetlist.state.value)
    }

    // endregion

    // region push

    @Test
    fun `boot schedules the device registration when logged in`() = runTest {
        coEvery { authSession.isLoggedIn() } returns true

        viewModel.initialize()
        advanceUntilIdle()

        assertEquals(1, pushScheduler.scheduleCalls)
    }

    @Test
    fun `boot does not register a logged-out device`() = runTest {
        viewModel.initialize()
        advanceUntilIdle()

        assertEquals(0, pushScheduler.scheduleCalls)
    }

    @Test
    fun `login schedules the device registration`() = runTest {
        viewModel.initialize()
        advanceUntilIdle()

        authEventsFlow.emit(AuthEventBus.Event.LoginSuccess)
        advanceUntilIdle()

        assertEquals(1, pushScheduler.scheduleCalls)
    }

    @Test
    fun `logout unregisters the device before clearing the session`() = runTest {
        var unregisteredBeforeLogout = false
        coEvery { logoutUseCase() } answers { unregisteredBeforeLogout = devices.unregistered.isNotEmpty() }

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(listOf("token-1"), devices.unregistered)
        assertTrue(unregisteredBeforeLogout)
    }

    @Test
    fun `logout completes when unregistering fails`() = runTest {
        devices.unregisterResult = Result.failure(AppError.Network())

        viewModel.events.test {
            viewModel.logout()
            advanceUntilIdle()
            assertEquals(CoreViewModel.CoreEvent.LogoutSuccess, awaitItem())
        }
        coVerify { logoutUseCase() }
    }

    // endregion
}
