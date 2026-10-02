package com.ipb.castelobranco.core.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.RefreshAccessUseCase
import com.ipb.castelobranco.core.domain.auth.AuthEventBus
import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.domain.repository.MembersRepository
import com.ipb.castelobranco.core.domain.session.SessionScopedCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.usecase.GetMonthlyBirthdaysUseCase
import com.ipb.castelobranco.core.domain.usecase.PreloadDataUseCase
import com.ipb.castelobranco.features.auth.data.local.AuthSession
import com.ipb.castelobranco.features.auth.domain.usecase.LogoutUseCase
import com.ipb.castelobranco.features.schedule.domain.repository.ScheduleRepository
import com.ipb.castelobranco.features.bible.domain.usecase.BibleAutoDownloadUseCase
import com.ipb.castelobranco.features.gallery.domain.usecase.GalleryAutoDownloadUseCase
import com.ipb.castelobranco.features.profile.domain.usecase.FetchProfileUseCase
import com.ipb.castelobranco.core.domain.setlist.SyncSundaySetlistUseCase
import com.ipb.castelobranco.core.domain.worship.ObserveWorshipAccessUseCase
import com.ipb.castelobranco.core.data.app.AppForegroundState
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.push.PushRegistrationScheduler
import com.ipb.castelobranco.core.domain.push.UnregisterDeviceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CoreViewModel @Inject constructor(
    private val preloadDataUseCase: PreloadDataUseCase,
    private val authSession: AuthSession,
    private val fetchProfileUseCase: FetchProfileUseCase,
    private val authEventBus: AuthEventBus,
    private val logoutUseCase: LogoutUseCase,
    private val galleryAutoDownload: GalleryAutoDownloadUseCase,
    private val bibleAutoDownload: BibleAutoDownloadUseCase,
    private val bibleRepository: com.ipb.castelobranco.features.bible.domain.repository.BibleRepository,
    private val scheduleRepository: ScheduleRepository,
    private val getMonthlyBirthdaysUseCase: GetMonthlyBirthdaysUseCase,
    private val membersRepository: MembersRepository,
    private val sessionScopedCaches: Set<@JvmSuppressWildcards SessionScopedCache>,
    observeAccess: ObserveAccessUseCase,
    private val refreshAccess: RefreshAccessUseCase,
    private val syncSundaySetlist: SyncSundaySetlistUseCase,
    private val observeWorshipAccess: ObserveWorshipAccessUseCase,
    private val pushRegistration: PushRegistrationScheduler,
    private val unregisterDevice: UnregisterDeviceUseCase,
    private val appForeground: AppForegroundState,
) : ViewModel() {

    sealed interface CoreEvent {
        data object LogoutSuccess : CoreEvent
    }

    private val _events = Channel<CoreEvent>(capacity = Channel.Factory.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _isPreloading = MutableStateFlow(false)
    val isPreloading: StateFlow<Boolean> = _isPreloading.asStateFlow()

    /** The disk caches (profile and access included) are in memory: notification taps can be routed. */
    private val _isBootReady = MutableStateFlow(false)
    val isBootReady: StateFlow<Boolean> = _isBootReady.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _birthdays = MutableStateFlow<List<Birthday>>(emptyList())
    val birthdays: StateFlow<List<Birthday>> = _birthdays.asStateFlow()

    /** Any role in the profile opens the management panel; what it shows inside is per level. */
    val canOpenPanel: StateFlow<Boolean> = observeAccess()
        .map { it.hasAnyRole }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Opens the register screen from a "Confirmar músicas de domingo" notification. */
    val canManageSongs: StateFlow<Boolean> = observeAccess()
        .map { it.allows(Scope.SONGS, AccessLevel.MANAGE) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var initialized = false

    /**
     * Idempotente: chamada tanto pelo AppNavHost (garante o boot mesmo quando o processo e
     * recriado direto numa rota interna) quanto pela CoreView.
     */
    fun initialize() {
        if (initialized) return
        initialized = true

        // Observa estado de login
        viewModelScope.launch {
            var wasLoggedIn = false
            authSession.isLoggedInFlow.collect { logged ->
                // A sessão pode cair sem logout(): o TokenAuthenticator limpa os tokens quando o
                // refresh falha. Dado que só vale para a sessão não pode sobreviver a isso.
                if (wasLoggedIn && !logged) clearSessionScopedCaches()
                wasLoggedIn = logged
                _isLoggedIn.value = logged
            }
        }

        // Reage ao login bem-sucedido para sincronizar dados de perfil
        viewModelScope.launch {
            authEventBus.events.collect { event ->
                when (event) {
                    AuthEventBus.Event.LoginSuccess -> {
                        pushRegistration.schedule()
                        refreshProfileOnAppOpen()
                        // A galeria só é acessível a membros: antes do login não havia o que sincronizar.
                        launch { galleryAutoDownload.onLoginSuccess() }
                    }
                    // O perfil guardado pode estar desatualizado: um papel removido vale já no servidor.
                    AuthEventBus.Event.PermissionDenied -> launch {
                        runCatching { refreshAccess() }
                            .onFailure { Timber.w(it, "Access refresh after 403 failed") }
                    }
                }
            }
        }

        // Quem sai do Louvor perde o repertório guardado (só a transição: no boot o perfil ainda carrega).
        viewModelScope.launch {
            var wasWorshipMember = false
            observeWorshipAccess().collect { access ->
                if (wasWorshipMember && !access.isWorshipMember) syncSetlist()
                wasWorshipMember = access.isWorshipMember
            }
        }

        // Observa aniversariantes
        viewModelScope.launch {
            getMonthlyBirthdaysUseCase.observe().collect { state ->
                _birthdays.value = when (state) {
                    is SnapshotState.Data -> state.value
                    else -> emptyList()
                }
            }
        }

        // Inicialização em cascata: Primeiro Cache (Rápido), depois Rede (Lento)
        startAppInitialization()
    }

    private fun startAppInitialization() {
        viewModelScope.launch {
            Timber.d("App initialization started")
            _isPreloading.value = true

            // 1 & 2. PRELOAD (disk) + REFRESH (network) delegated to use case
            preloadDataUseCase(onDiskLoaded = { _isBootReady.value = true })

            // Bíblia: preload do cache + auto-download se faltar tradução (WiFi only)
            runCatching { bibleRepository.preload() }
                .onFailure { Timber.w(it, "Bible preload failed") }
            bibleAutoDownload.triggerIfNeeded()

            // Perfil é um caso à parte pois depende de login
            val profileRefresh = refreshProfileOnAppOpen()

            // Idempotente no servidor: registra também quem fez login antes desta versão.
            if (authSession.isLoggedIn()) pushRegistration.schedule()

            // Repertório de domingo: depende do perfil (só membros do Louvor) e é o fallback do push.
            // Roda depois do perfil sem segurar o fim do boot.
            launch {
                profileRefresh.join()
                syncSetlist()
            }

            _isPreloading.value = false
            Timber.d("App initialization completed")
        }
    }

    private fun refreshProfileOnAppOpen() =
        viewModelScope.launch {
            if (!authSession.isLoggedIn()) return@launch

            runCatching {
                fetchProfileUseCase.refresh()

                val profileState = fetchProfileUseCase.observe().first()
                if (profileState is SnapshotState.Data) {
                    val url = profileState.value.photoUrl
                    if (!url.isNullOrBlank()) {
                        fetchProfileUseCase.downloadAndPersistPhoto(url)
                    }
                }
            }.onFailure { Timber.w(it, "Profile refresh on app open failed") }
        }

    /**
     * Activity `ON_START`: the app opened or came back to the foreground. The gallery follows the
     * server from here — only with a session, since the gallery is restricted to members.
     */
    fun onAppForeground() {
        appForeground.isForeground = true
        viewModelScope.launch {
            if (!authSession.isLoggedIn()) return@launch
            runCatching { galleryAutoDownload.onAppForeground() }
                .onFailure { Timber.w(it, "Gallery sync on foreground failed") }
        }
        // O boot já sincroniza; a volta do background é o fallback de um push perdido.
        if (initialized && !_isPreloading.value) viewModelScope.launch { syncSetlist() }
    }

    private suspend fun syncSetlist() {
        runCatching { syncSundaySetlist() }
            .onFailure { Timber.w(it, "Sunday setlist sync failed") }
            .getOrNull()
            ?.onFailure { Timber.w(it, "Sunday setlist read failed") }
    }

    /** Whether a "Confirmar músicas de domingo" tap may open the register screen. */
    suspend fun canOpenSundayConfirmation(): Boolean = authSession.isLoggedIn() && canManageSongs.value

    /** Activity `ON_STOP`: notifications are shown again for messages that arrive now. */
    fun onAppBackground() {
        appForeground.isForeground = false
    }

    fun refreshLoginState() {
        viewModelScope.launch {
            _isLoggedIn.value = authSession.isLoggedIn()
        }
    }

    fun logout() {
        viewModelScope.launch {
            Timber.d("Logout started")
            // Antes de tudo: a chamada ainda precisa do token de acesso. Nunca impede o logout.
            unregisterDevice()
            fetchProfileUseCase.clearLocalPhoto()
            fetchProfileUseCase.clearSnapshot()
            scheduleRepository.clearScheduleCache()
            membersRepository.clearBirthdaysCache()
            galleryAutoDownload.clearOnLogout()
            clearSessionScopedCaches()
            logoutUseCase()
            Timber.d("Logout completed")
            _events.trySend(CoreEvent.LogoutSuccess)
        }
    }

    private suspend fun clearSessionScopedCaches() {
        sessionScopedCaches.forEach { cache ->
            runCatching { cache.clear() }
                .onFailure { Timber.w(it, "Session cache clear failed") }
        }
    }
}
