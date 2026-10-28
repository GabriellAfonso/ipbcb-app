package com.ipb.castelobranco.core.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.CustomButton
import com.ipb.castelobranco.core.presentation.components.Highlight
import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.presentation.components.HighlightBirthdays
import com.ipb.castelobranco.core.presentation.components.HighlightBirthdaysLoading
import com.ipb.castelobranco.core.presentation.components.HighlightEvent
import com.ipb.castelobranco.core.presentation.components.HighlightEventUi
import com.ipb.castelobranco.core.presentation.components.HighlightEvents
import com.ipb.castelobranco.core.presentation.components.HighlightEventsLoading
import com.ipb.castelobranco.core.presentation.components.HighlightScheduleLoading
import com.ipb.castelobranco.core.presentation.components.HighlightScheduleUnavailable
import com.ipb.castelobranco.core.presentation.components.HighlightSundaySchedule
import com.ipb.castelobranco.core.presentation.viewmodel.CoreViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.ImageLoader
import com.ipb.castelobranco.features.events.presentation.viewmodel.EventsHighlightViewModel
import com.ipb.castelobranco.features.profile.presentation.viewmodel.ProfileViewModel
import com.ipb.castelobranco.features.schedule.presentation.components.NextScheduleUi
import com.ipb.castelobranco.features.schedule.presentation.viewmodel.ScheduleUiState
import com.ipb.castelobranco.features.schedule.presentation.viewmodel.ScheduleViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val GRID_EDGE_PADDING_RATIO = 0.055f
private const val GRID_GAP_RATIO = 0.064f
private const val GRID_ROW_VERTICAL_PADDING_RATIO = 0.041f

/**
 * Agrupador de permissões e estado de autenticação
 */
data class UserAuthState(
    val isLoggedIn: Boolean = false,
    /** The profile lists at least one role (Admin, Liderança, Mídia). */
    val canOpenPanel: Boolean = false,
)

private const val MANAGEMENT_PANEL_LABEL = "Painel de Gestão"

@Composable
fun CoreView(
    onNavigateToAuth: () -> Unit,
    onNavigateToWorshipHub: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToHymnal: () -> Unit,
    onNavigateToBible: () -> Unit,
    onNavigateToStudies: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToBirthdays: () -> Unit,
    onNavigateToEvent: (Int) -> Unit,
    onLogoutSuccess: () -> Unit,
    viewModel: CoreViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    scheduleViewModel: ScheduleViewModel = hiltViewModel(),
    eventsViewModel: EventsHighlightViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.initialize() }
    LaunchedEffect(Unit) { profileViewModel.initialize() }

    val isLoggedIn      by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val canOpenPanel    by viewModel.canOpenPanel.collectAsStateWithLifecycle()
    val nextSection     by scheduleViewModel.nextSection.collectAsStateWithLifecycle()
    val scheduleState   by scheduleViewModel.uiState.collectAsStateWithLifecycle()
    val birthdays       by viewModel.birthdays.collectAsStateWithLifecycle()
    val eventPages      by eventsViewModel.pages.collectAsStateWithLifecycle()
    val isSyncing       by viewModel.isSyncing.collectAsStateWithLifecycle()

    // Back from management (or anywhere) the carousel shows what changed; a login change swaps the
    // members-only events in or out (`specs/events/spec.md` §2.1).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { eventsViewModel.refresh() }
    LaunchedEffect(isLoggedIn) { eventsViewModel.onLoginStateChanged(isLoggedIn) }

    val authState = UserAuthState(
        isLoggedIn   = isLoggedIn,
        canOpenPanel = canOpenPanel,
    )

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is CoreViewModel.CoreEvent.LogoutSuccess -> {
                    onLogoutSuccess()
                    Toast.makeText(context, "Sessão encerrada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    CoreScreen(
        onNavigateToAuth       = onNavigateToAuth,
        onNavigateToWorshipHub = onNavigateToWorshipHub,
        onNavigateToSchedule   = onNavigateToSchedule,
        onNavigateToGallery    = onNavigateToGallery,
        onNavigateToHymnal     = onNavigateToHymnal,
        onNavigateToBible      = onNavigateToBible,
        onNavigateToStudies    = onNavigateToStudies,
        onNavigateToSettings   = onNavigateToSettings,
        onNavigateToAdmin      = onNavigateToAdmin,
        onNavigateToBirthdays  = onNavigateToBirthdays,
        onNavigateToEvent      = onNavigateToEvent,
        authState              = authState,
        highlights             = HighlightsUi(
            nextSection = nextSection,
            // Loading, or an error the sync may still fix (a login replaces the anonymous 401).
            hasScheduleData = scheduleState is ScheduleUiState.Success || scheduleState is ScheduleUiState.Empty,
            birthdays = birthdays,
            eventPages = eventPages,
            isSyncing = isSyncing,
        ),
        eventImageLoader       = eventsViewModel.imageLoader,
        onLogout               = viewModel::logout,
    )
}

@Composable
fun CoreScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToWorshipHub: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToHymnal: () -> Unit,
    onNavigateToBible: () -> Unit,
    onNavigateToStudies: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToBirthdays: () -> Unit,
    onNavigateToEvent: (Int) -> Unit,
    authState: UserAuthState,
    highlights: HighlightsUi,
    eventImageLoader: ImageLoader,
    onLogout: () -> Unit,
) {
    NavigationDrawer(
        onNavigateToAuth     = onNavigateToAuth,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToAdmin    = onNavigateToAdmin,
        authState            = authState,
        onLogout             = onLogout,
    ) { openDrawer ->
        BaseScreen(
            tabName           = stringResource(R.string.app_name),
            onMenuClick       = openDrawer,
            showAccountAction = true,
        ) { innerPadding ->
            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                Highlight(
                    pages = buildHighlightPages(
                        highlights       = highlights,
                        eventImageLoader = eventImageLoader,
                        onBirthdaysClick = onNavigateToBirthdays,
                        onScheduleClick  = onNavigateToSchedule,
                        onEventClick     = onNavigateToEvent,
                    )
                )

                // The highlight's page dots take 6dp: the grid stays where it was.
                Spacer(modifier = Modifier.height(54.dp))
                ButtonGrid(
                    onNavigateToWorshipHub = onNavigateToWorshipHub,
                    onNavigateToSchedule   = onNavigateToSchedule,
                    onNavigateToGallery    = onNavigateToGallery,
                    onNavigateToHymnal     = onNavigateToHymnal,
                    onNavigateToBible      = onNavigateToBible,
                    onNavigateToStudies    = onNavigateToStudies,
                )
            }
        }
    }
}

/**
 * What the carousel shows. A page with nothing to show while [isSyncing] reads as loading, never as empty: the
 * network may still bring its data.
 *
 * @param birthdays null while nothing is loaded. @param eventPages null while nothing is loaded.
 */
data class HighlightsUi(
    val nextSection: NextScheduleUi? = null,
    val hasScheduleData: Boolean = false,
    val birthdays: List<Birthday>? = null,
    val eventPages: List<HighlightEventUi>? = null,
    val isSyncing: Boolean = false,
)

private fun buildHighlightPages(
    highlights: HighlightsUi,
    eventImageLoader: ImageLoader,
    onBirthdaysClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onEventClick: (Int) -> Unit,
): List<@Composable () -> Unit> = buildList {
    val (nextSection, hasScheduleData, birthdays, eventPages, isSyncing) = highlights

    when {
        birthdays != null -> add { HighlightBirthdays(birthdays, onClick = onBirthdaysClick) }
        isSyncing -> add { HighlightBirthdaysLoading(onClick = onBirthdaysClick) }
        else -> add { HighlightBirthdays(emptyList(), onClick = onBirthdaysClick) }
    }

    when {
        nextSection != null -> add { HighlightSundaySchedule(schedule = nextSection, onClick = onScheduleClick) }
        !hasScheduleData && isSyncing -> add { HighlightScheduleLoading(onClick = onScheduleClick) }
        else -> add { HighlightScheduleUnavailable(onClick = onScheduleClick) }
    }

    when {
        !eventPages.isNullOrEmpty() -> eventPages.forEach { page ->
            add { HighlightEvent(page = page, imageLoader = eventImageLoader, onClick = { onEventClick(page.id) }) }
        }
        eventPages == null && isSyncing -> add { HighlightEventsLoading() }
        else -> add { HighlightEvents() }
    }
}

@Composable
fun NavigationDrawer(
    onNavigateToAuth: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    authState: UserAuthState,
    onLogout: () -> Unit,
    content: @Composable (openDrawer: () -> Unit) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = {
            // Força recomposição se o status de login mudar
            key(authState.isLoggedIn) {
                ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.8f)) {
                    DrawerContent(
                        onNavigateToAuth     = onNavigateToAuth,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToAdmin    = onNavigateToAdmin,
                        authState            = authState,
                        onLogout             = onLogout,
                    ) { action ->
                        scope.launch {
                            drawerState.close()
                            action()
                        }
                    }
                }
            }
        },
        content = { content { scope.launch { drawerState.open() } } },
    )
}

@Composable
fun DrawerContent(
    onNavigateToAuth: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    authState: UserAuthState,
    onLogout: () -> Unit,
    onItemClick: (action: () -> Unit) -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = Modifier.padding(10.dp)) {
        if (!authState.isLoggedIn) {
            DrawerMenuItem(
                iconRes   = R.drawable.ic_login,
                label     = "Entrar",
                textColor = textColor,
            ) {
                onItemClick { onNavigateToAuth() }
            }
        }

        if (authState.isLoggedIn && authState.canOpenPanel) {
            DrawerMenuItem(
                iconRes   = R.drawable.ic_admin_panel,
                label     = MANAGEMENT_PANEL_LABEL,
                textColor = textColor,
            ) {
                onItemClick { onNavigateToAdmin() }
            }
        }

        DrawerMenuItem(
            iconRes   = R.drawable.ic_settings,
            label     = "Configurações",
            textColor = textColor,
        ) {
            onItemClick { onNavigateToSettings() }
        }

        if (authState.isLoggedIn) {
            DrawerMenuItem(
                iconRes   = R.drawable.ic_logout,
                label     = "Logout",
                textColor = textColor,
            ) {
                onItemClick { onLogout() }
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    iconRes: Int,
    label: String,
    textColor: Color,
    onClick: () -> Unit,
) {
    TextButton(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = textColor),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter            = painterResource(id = iconRes),
                contentDescription = null,
                tint               = textColor,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text      = label,
                modifier  = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                color     = textColor,
            )
        }
    }
}

@Composable
fun ButtonGrid(
    onNavigateToWorshipHub: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToHymnal: () -> Unit,
    onNavigateToBible: () -> Unit,
    onNavigateToStudies: () -> Unit,
) {
    val iconColor = MaterialTheme.colorScheme.primaryContainer
    val buttons = listOf(
        ButtonInfo(R.drawable.ic_worshiphub, "Louvor", iconColor, onNavigateToWorshipHub),
        ButtonInfo(R.drawable.ic_schedule,   "Escala",      iconColor, onNavigateToSchedule),
        ButtonInfo(R.drawable.ic_galery,     "Galeria",     iconColor, onNavigateToGallery),
        ButtonInfo(R.drawable.ic_sarca_ipb,  "Hinário",     iconColor, onNavigateToHymnal),
        ButtonInfo(R.drawable.ic_studies,    "Estudos",     iconColor, onNavigateToStudies),
        ButtonInfo(R.drawable.ic_bible,      "Bíblia",      iconColor, onNavigateToBible),
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val edgePadding = maxWidth * GRID_EDGE_PADDING_RATIO
        val gap = maxWidth * GRID_GAP_RATIO
        val rowVerticalPadding = maxWidth * GRID_ROW_VERTICAL_PADDING_RATIO
        val buttonSize = (maxWidth - edgePadding * 2 - gap * 2) / 3

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = edgePadding),
        ) {
            ButtonRow(buttons.subList(0, 3), gap, rowVerticalPadding, buttonSize)
            ButtonRow(buttons.subList(3, 6), gap, rowVerticalPadding, buttonSize)
        }
    }
}

@Composable
private fun ButtonRow(
    rowButtons: List<ButtonInfo>,
    gap: Dp,
    verticalPadding: Dp,
    buttonSize: Dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(gap),
        modifier              = Modifier.padding(vertical = verticalPadding),
    ) {
        rowButtons.forEach { button ->
            CustomButton(
                image           = painterResource(id = button.iconRes),
                text            = button.label,
                backgroundColor = button.color,
                onClick         = button.onClick,
                size            = buttonSize,
            )
        }
    }
}

data class ButtonInfo(
    val iconRes: Int,
    val label: String,
    val color: Color,
    val onClick: () -> Unit,
)
