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
import com.ipb.castelobranco.core.presentation.components.HighlightEvents
import com.ipb.castelobranco.core.presentation.components.HighlightScheduleUnavailable
import com.ipb.castelobranco.core.presentation.components.HighlightSundaySchedule
import com.ipb.castelobranco.core.presentation.viewmodel.CoreViewModel
import com.ipb.castelobranco.features.profile.presentation.viewmodel.ProfileViewModel
import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleSectionUi
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
    onLogoutSuccess: () -> Unit,
    viewModel: CoreViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    scheduleViewModel: ScheduleViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.initialize() }
    LaunchedEffect(Unit) { profileViewModel.initialize() }

    val isLoggedIn      by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val canOpenPanel    by viewModel.canOpenPanel.collectAsStateWithLifecycle()
    val nextSection     by scheduleViewModel.nextSection.collectAsStateWithLifecycle()
    val birthdays       by viewModel.birthdays.collectAsStateWithLifecycle()

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
        authState              = authState,
        nextSection            = nextSection,
        birthdays              = birthdays,
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
    authState: UserAuthState,
    nextSection: ScheduleSectionUi?,
    birthdays: List<Birthday>,
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

                Highlight(pages = buildHighlightPages(nextSection, birthdays))

                Spacer(modifier = Modifier.height(60.dp))
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

private fun buildHighlightPages(
    nextSection: ScheduleSectionUi?,
    birthdays: List<Birthday> = emptyList(),
): List<@Composable () -> Unit> = buildList {
    add { HighlightBirthdays(birthdays) }

    if (nextSection != null) {
        add { HighlightSundaySchedule(section = nextSection) }
    } else {
        add { HighlightScheduleUnavailable() }
    }

    add { HighlightEvents() }
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
