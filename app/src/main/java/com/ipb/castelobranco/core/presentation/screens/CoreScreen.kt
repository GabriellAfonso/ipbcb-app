package com.ipb.castelobranco.core.presentation.screens

import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.Highlight
import com.ipb.castelobranco.core.presentation.components.HighlightBirthdays
import com.ipb.castelobranco.core.presentation.components.HighlightBirthdaysLoading
import com.ipb.castelobranco.core.presentation.components.HighlightEvent
import com.ipb.castelobranco.core.presentation.components.HighlightEventUi
import com.ipb.castelobranco.core.presentation.components.HighlightScheduleLoading
import com.ipb.castelobranco.core.presentation.components.HighlightScheduleUnavailable
import com.ipb.castelobranco.core.presentation.components.HighlightSundaySchedule
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.core.presentation.theme.onPrimaryContainerLight
import com.ipb.castelobranco.core.presentation.viewmodel.CoreViewModel
import com.ipb.castelobranco.features.events.presentation.viewmodel.EventsHighlightViewModel
import com.ipb.castelobranco.features.profile.presentation.viewmodel.ProfileViewModel
import com.ipb.castelobranco.features.schedule.presentation.components.NextScheduleUi
import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleBandUi
import com.ipb.castelobranco.features.schedule.presentation.viewmodel.ScheduleUiState
import com.ipb.castelobranco.features.schedule.presentation.viewmodel.ScheduleViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Agrupador de permissões e estado de autenticação
 */
data class UserAuthState(
    val isLoggedIn: Boolean = false,
    /** The profile lists at least one role (Admin, Liderança, Mídia). */
    val canOpenPanel: Boolean = false,
)

private const val MANAGEMENT_PANEL_LABEL = "Painel de Gestão"
private const val DRAWER_WIDTH_FRACTION = 0.8f
private const val LANDSCAPE_DRAWER_WIDTH_FRACTION = 0.4f
private val DrawerMaxWidth = 360.dp
private const val OWN_SCHEDULE_LABEL = "Sua próxima escala"
private const val NEXT_SERVICE_LABEL = "Próximo culto"
private const val LOGIN_BUTTON_LABEL = "Entrar para ver sua escala"

private val BandCorner = 28.dp
private val BandHorizontalPadding = 20.dp
private val BandTopPadding = 12.dp
private val BandBottomPadding = 20.dp
private val BandCardCorner = 16.dp
private const val BAND_MUTED_ALPHA = 0.75f
private const val BAND_CARD_ALPHA = 0.10f
private const val BAND_CARD_BORDER_ALPHA = 0.14f
private const val BAND_CHEVRON_ALPHA = 0.7f
private val DateBadgeWidth = 44.dp
private val DateBadgeHeight = 48.dp
private val DateBadgeCorner = 12.dp

private val ShortcutMinGap = 16.dp

// The highlight card at full width, as `Highlight` draws it: 3:2 inside 16dp side margins, 8dp above and below, then
// the 6dp page dots; plus 16dp so it does not touch the shortcuts.
private const val HIGHLIGHT_ASPECT_RATIO = 3f / 2f
private val HighlightSideMargin = 16.dp
private val HighlightChrome = 8.dp * 2 + 6.dp + 16.dp
private val ShortcutGap = 10.dp
private val ShortcutEdgePadding = 16.dp
private val ShortcutCorner = 18.dp
private val ShortcutElevation = 6.dp
private const val SHORTCUT_COLUMNS = 3

/** Below this height (Material's compact height: a phone lying down) the home switches to its landscape layout. */
private const val COMPACT_HEIGHT_DP = 480
private val LandscapeBandCorner = 22.dp
private val LandscapeBandCardWidth = 300.dp
private val LandscapeSidePadding = 20.dp
private val LandscapeColumnGap = 16.dp
private const val LANDSCAPE_HIGHLIGHT_WEIGHT = 0.8f
private const val SHORTCUT_SHADOW_AMBIENT_ALPHA = 0.08f
private const val SHORTCUT_SHADOW_SPOT_ALPHA = 0.12f

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
    val scheduleBand    by scheduleViewModel.band.collectAsStateWithLifecycle()
    val profile         by profileViewModel.uiState.collectAsStateWithLifecycle()
    val birthdays       by viewModel.birthdays.collectAsStateWithLifecycle()
    val eventPages      by eventsViewModel.pages.collectAsStateWithLifecycle()
    val isSyncing       by viewModel.isSyncing.collectAsStateWithLifecycle()

    // Greeting and date follow the clock: read again whenever the home comes back.
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    // Back from management (or anywhere) the carousel shows what changed; a login change swaps the
    // members-only events in or out (`specs/events/spec.md` §2.1).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        now = LocalDateTime.now()
        eventsViewModel.refresh()
    }
    LaunchedEffect(isLoggedIn) { eventsViewModel.onLoginStateChanged(isLoggedIn) }

    val ownMemberId = profile.memberId.takeIf { isLoggedIn }
    LaunchedEffect(ownMemberId) { scheduleViewModel.setMemberId(ownMemberId) }

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
        header                 = buildHomeHeader(
            today      = now.toLocalDate(),
            hour       = now.hour,
            isLoggedIn = isLoggedIn,
            firstName  = profile.firstName,
            schedule   = scheduleBand,
        ),
        highlights             = HighlightsUi(
            nextSection = nextSection,
            // Loading, or an error the sync may still fix (a login replaces the anonymous 401).
            hasScheduleData = scheduleState is ScheduleUiState.Success || scheduleState is ScheduleUiState.Empty,
            ownMemberId = ownMemberId,
            birthdays = birthdays,
            todayDay = now.dayOfMonth,
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
    header: HomeHeaderUi,
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
            val shortcuts = listOf(
                Shortcut(R.drawable.ic_worshiphub, "Louvor",  onNavigateToWorshipHub),
                Shortcut(R.drawable.ic_schedule,   "Escala",  onNavigateToSchedule),
                Shortcut(R.drawable.ic_galery,     "Galeria", onNavigateToGallery),
                Shortcut(R.drawable.ic_sarca_ipb,  "Hinário", onNavigateToHymnal),
                Shortcut(R.drawable.ic_studies,    "Estudos", onNavigateToStudies),
                Shortcut(R.drawable.ic_bible,      "Bíblia",  onNavigateToBible),
            )
            val highlight: @Composable () -> Unit = {
                Highlight(
                    pages = buildHighlightPages(
                        highlights       = highlights,
                        eventImageLoader = eventImageLoader,
                        onBirthdaysClick = onNavigateToBirthdays,
                        onScheduleClick  = onNavigateToSchedule,
                        onEventClick     = onNavigateToEvent,
                    )
                )
            }
            if (LocalConfiguration.current.screenHeightDp < COMPACT_HEIGHT_DP) {
                HomeLandscapeLayout(
                    innerPadding = innerPadding,
                    band = {
                        HomeBandRow(
                            header          = header,
                            onScheduleClick = onNavigateToSchedule,
                            onLoginClick    = onNavigateToAuth,
                        )
                    },
                    shortcuts = { ShortcutGrid(shortcuts = shortcuts, size = ShortcutSize.Compact) },
                    highlight = highlight,
                )
            } else {
                HomeLayout(
                    innerPadding = innerPadding,
                    band = {
                        HomeBand(
                            header          = header,
                            onScheduleClick = onNavigateToSchedule,
                            onLoginClick    = onNavigateToAuth,
                        )
                    },
                    shortcuts = {
                        ShortcutGrid(
                            shortcuts = shortcuts,
                            modifier  = Modifier.padding(horizontal = ShortcutEdgePadding),
                        )
                    },
                    highlight = highlight,
                )
            }
        }
    }
}

/**
 * Lying down (`specs/core/spec.md` §6.3, "Celular deitado"): the band as one line on top; below it, the shortcuts on
 * the left and the highlight on the right, centered and sized by the height left.
 */
@Composable
private fun HomeLandscapeLayout(
    innerPadding: PaddingValues,
    band: @Composable () -> Unit,
    shortcuts: @Composable () -> Unit,
    highlight: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        band()
        Row(
            modifier              = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = LandscapeSidePadding, end = LandscapeSidePadding, top = 10.dp, bottom = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LandscapeColumnGap),
        ) {
            Box(modifier = Modifier.weight(1f)) { shortcuts() }
            Box(
                modifier         = Modifier
                    .weight(LANDSCAPE_HIGHLIGHT_WEIGHT)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                highlight()
            }
        }
    }
}

/**
 * No scroll, on any screen height. The band takes its own height; the shortcut grid is centered on the screen
 * (status bar to navigation bar); the highlight is centered in what is left below. When centering does not fit,
 * the grid moves the least it can: it keeps [ShortcutMinGap] below the band, and the highlight keeps its full width.
 */
@Composable
private fun HomeLayout(
    innerPadding: PaddingValues,
    band: @Composable () -> Unit,
    shortcuts: @Composable () -> Unit,
    highlight: @Composable () -> Unit,
) {
    Layout(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        content  = {
            band()
            shortcuts()
            Box(contentAlignment = Alignment.Center) { highlight() }
        },
    ) { measurables, constraints ->
        val (bandMeasurable, gridMeasurable, highlightMeasurable) = measurables
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val fullWidth = Constraints(minWidth = width, maxWidth = width)

        val band = bandMeasurable.measure(fullWidth)
        val grid = gridMeasurable.measure(fullWidth)

        // The screen's center in this layout, which starts below the top bar and ends above the navigation bar.
        val screenCenter = (height + innerPadding.calculateBottomPadding().roundToPx() -
            innerPadding.calculateTopPadding().roundToPx()) / 2
        val highlightFullHeight = ((width - (HighlightSideMargin * 2).roundToPx()) / HIGHLIGHT_ASPECT_RATIO).toInt() +
            HighlightChrome.roundToPx()
        val gridTop = (screenCenter - grid.height / 2)
            .coerceAtMost(height - grid.height - highlightFullHeight)
            .coerceAtLeast(band.height + ShortcutMinGap.roundToPx())

        val highlightBox = highlightMeasurable.measure(
            Constraints.fixed(width, (height - gridTop - grid.height).coerceAtLeast(0))
        )
        layout(width, height) {
            band.place(0, 0)
            grid.place(0, gridTop)
            highlightBox.place(0, gridTop + grid.height)
        }
    }
}

@Composable
private fun HomeBand(
    header: HomeHeaderUi,
    onScheduleClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = BandCorner, bottomEnd = BandCorner))
            .background(ipbGreen)
            .padding(
                start  = BandHorizontalPadding,
                end    = BandHorizontalPadding,
                top    = BandTopPadding,
                bottom = BandBottomPadding,
            ),
    ) {
        Text(
            text  = header.dateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = BAND_MUTED_ALPHA),
        )
        Text(
            text       = header.greeting,
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color      = Color.White,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis,
        )
        when {
            !header.isLoggedIn -> {
                Spacer(modifier = Modifier.height(12.dp))
                LoginButton(onClick = onLoginClick)
            }
            header.schedule != null -> {
                Spacer(modifier = Modifier.height(12.dp))
                ScheduleBandCard(band = header.schedule, onClick = onScheduleClick)
            }
        }
    }
}

/** The band as one line, for a phone lying down: date and greeting on the left, the schedule card on the right. */
@Composable
private fun HomeBandRow(
    header: HomeHeaderUi,
    onScheduleClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = LandscapeBandCorner, bottomEnd = LandscapeBandCorner))
            .background(ipbGreen)
            .padding(start = BandHorizontalPadding, end = BandHorizontalPadding, top = 8.dp, bottom = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = header.dateLabel,
                style    = MaterialTheme.typography.bodySmall,
                color    = Color.White.copy(alpha = BAND_MUTED_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text       = header.greeting,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
        }
        when {
            !header.isLoggedIn -> LoginButton(onClick = onLoginClick)
            header.schedule != null -> ScheduleBandCard(
                band     = header.schedule,
                onClick  = onScheduleClick,
                modifier = Modifier.width(LandscapeBandCardWidth),
            )
        }
    }
}

@Composable
private fun LoginButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors  = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ipbGreen),
    ) {
        Text(text = LOGIN_BUTTON_LABEL, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ScheduleBandCard(
    band: ScheduleBandUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val shape = RoundedCornerShape(BandCardCorner)
    Row(
        modifier          = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = BAND_CARD_ALPHA))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = BAND_CARD_BORDER_ALPHA)), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateBadge(band = band)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = if (band.isOwn) OWN_SCHEDULE_LABEL else NEXT_SERVICE_LABEL,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = BAND_MUTED_ALPHA),
            )
            Text(
                text       = band.title,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = band.detail,
                style    = MaterialTheme.typography.bodySmall,
                color    = onPrimaryContainerLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint               = Color.White.copy(alpha = BAND_CHEVRON_ALPHA),
        )
    }
}

/** Month and day; white with green text on the user's own date, translucent on the next service. */
@Composable
private fun DateBadge(band: ScheduleBandUi) {
    val background = if (band.isOwn) Color.White else Color.White.copy(alpha = BAND_CARD_BORDER_ALPHA)
    val content = if (band.isOwn) ipbGreen else Color.White
    Column(
        modifier            = Modifier
            .size(width = DateBadgeWidth, height = DateBadgeHeight)
            .clip(RoundedCornerShape(DateBadgeCorner))
            .background(background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = band.monthLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
            color = content)
        Text(text = band.day.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = content)
    }
}

/**
 * What the carousel shows. A page with nothing to show while [isSyncing] reads as loading, never as empty: the
 * network may still bring its data.
 *
 * @param birthdays null while nothing is loaded. @param eventPages null while nothing is loaded.
 * @param ownMemberId the logged-in user's member record, marked on the schedule page.
 * @param todayDay today's day of the month, highlighted on the birthdays page.
 */
data class HighlightsUi(
    val nextSection: NextScheduleUi? = null,
    val hasScheduleData: Boolean = false,
    val ownMemberId: Long? = null,
    val birthdays: List<Birthday>? = null,
    val todayDay: Int? = null,
    val eventPages: List<HighlightEventUi>? = null,
    val isSyncing: Boolean = false,
)

/** Schedule, birthdays, then one page per event. Events are rare: no event means no event page at all. */
private fun buildHighlightPages(
    highlights: HighlightsUi,
    eventImageLoader: ImageLoader,
    onBirthdaysClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onEventClick: (Int) -> Unit,
): List<@Composable () -> Unit> = buildList {
    val nextSection = highlights.nextSection
    val birthdays = highlights.birthdays

    when {
        nextSection != null -> add {
            HighlightSundaySchedule(
                schedule = nextSection,
                ownMemberId = highlights.ownMemberId,
                onClick = onScheduleClick,
            )
        }
        !highlights.hasScheduleData && highlights.isSyncing -> add { HighlightScheduleLoading(onClick = onScheduleClick) }
        else -> add { HighlightScheduleUnavailable(onClick = onScheduleClick) }
    }

    when {
        birthdays != null -> add {
            HighlightBirthdays(birthdays, todayDay = highlights.todayDay, onClick = onBirthdaysClick)
        }
        highlights.isSyncing -> add { HighlightBirthdaysLoading(onClick = onBirthdaysClick) }
        else -> add { HighlightBirthdays(emptyList(), onClick = onBirthdaysClick) }
    }

    highlights.eventPages.orEmpty().forEach { page ->
        add { HighlightEvent(page = page, imageLoader = eventImageLoader, onClick = { onEventClick(page.id) }) }
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
    // Turning the phone changes the sheet's width, and the drawer would keep the old offset and show up open: a
    // new width starts a new, closed state.
    val configuration = LocalConfiguration.current
    val drawerState = key(configuration.screenWidthDp) { rememberDrawerState(DrawerValue.Closed) }
    val drawerWidthFraction =
        if (configuration.screenHeightDp < COMPACT_HEIGHT_DP) LANDSCAPE_DRAWER_WIDTH_FRACTION else DRAWER_WIDTH_FRACTION
    val scope       = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = {
            // Força recomposição se o status de login mudar
            key(authState.isLoggedIn) {
                ModalDrawerSheet(modifier = Modifier.fillMaxWidth(drawerWidthFraction).widthIn(max = DrawerMaxWidth)) {
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

data class Shortcut(
    @DrawableRes val iconRes: Int,
    val label: String,
    val onClick: () -> Unit,
)

/** Card, icon circle and icon sizes: regular standing up, compact lying down. */
private enum class ShortcutSize(val height: Dp, val circle: Dp, val icon: Dp) {
    Regular(height = 90.dp, circle = 38.dp, icon = 21.dp),
    Compact(height = 80.dp, circle = 34.dp, icon = 19.dp),
}

/** Six equal cards in 3 columns. */
@Composable
private fun ShortcutGrid(
    shortcuts: List<Shortcut>,
    size: ShortcutSize = ShortcutSize.Regular,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ShortcutGap),
    ) {
        shortcuts.chunked(SHORTCUT_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(ShortcutGap)) {
                row.forEach { shortcut ->
                    ShortcutCard(shortcut = shortcut, size = size, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ShortcutCard(shortcut: Shortcut, size: ShortcutSize, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val shape = RoundedCornerShape(ShortcutCorner)
    Column(
        modifier            = modifier
            .height(size.height)
            .shadow(
                elevation    = ShortcutElevation,
                shape        = shape,
                ambientColor = Color.Black.copy(alpha = SHORTCUT_SHADOW_AMBIENT_ALPHA),
                spotColor    = Color.Black.copy(alpha = SHORTCUT_SHADOW_SPOT_ALPHA),
            )
            .clip(shape)
            .background(highlightCardColor())
            .clickable {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                shortcut.onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier         = Modifier
                .size(size.circle)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter            = painterResource(id = shortcut.iconRes),
                contentDescription = null,
                modifier           = Modifier.size(size.icon),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text      = shortcut.label,
            style     = if (size == ShortcutSize.Compact) MaterialTheme.typography.labelMedium
                        else MaterialTheme.typography.labelLarge,
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, name = "Home band - own date")
@Composable
private fun HomeBandOwnPreview() {
    HomeBand(
        header = HomeHeaderUi(
            dateLabel  = "Quarta, 28 de outubro",
            greeting   = "Boa noite, Gabriel",
            isLoggedIn = true,
            schedule   = ScheduleBandUi(true, "NOV", 15, "Domingo, 15/11 · 18h", "Faltam 18 dias"),
        ),
        onScheduleClick = {},
        onLoginClick    = {},
    )
}

@Preview(showBackground = true, name = "Home band - next service")
@Composable
private fun HomeBandNextServicePreview() {
    HomeBand(
        header = HomeHeaderUi(
            dateLabel  = "Quarta, 28 de outubro",
            greeting   = "Boa noite, Gabriel",
            isLoggedIn = true,
            schedule   = ScheduleBandUi(false, "OUT", 29, "Quinta, 29/10 · 19h30", "Escalado: Marcos Vinícius"),
        ),
        onScheduleClick = {},
        onLoginClick    = {},
    )
}

@Preview(showBackground = true, name = "Home band - visitor")
@Composable
private fun HomeBandVisitorPreview() {
    HomeBand(
        header = HomeHeaderUi(
            dateLabel  = "Quarta, 28 de outubro",
            greeting   = VISITOR_GREETING,
            isLoggedIn = false,
            schedule   = null,
        ),
        onScheduleClick = {},
        onLoginClick    = {},
    )
}
