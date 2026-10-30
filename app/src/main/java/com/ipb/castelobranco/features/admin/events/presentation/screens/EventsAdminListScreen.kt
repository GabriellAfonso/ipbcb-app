package com.ipb.castelobranco.features.admin.events.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.GreenTab
import com.ipb.castelobranco.core.presentation.components.GreenTabBar
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.events.domain.model.EventStatusFilter
import com.ipb.castelobranco.features.admin.events.presentation.components.EventAdminRow
import com.ipb.castelobranco.features.admin.events.presentation.state.EventAdminItemUi
import com.ipb.castelobranco.features.admin.events.presentation.state.EventMonthUi
import com.ipb.castelobranco.features.admin.events.presentation.state.EventsAdminEvent
import com.ipb.castelobranco.features.admin.events.presentation.state.EventsAdminListUiState
import com.ipb.castelobranco.features.admin.events.presentation.viewmodel.EventsAdminListViewModel

@Composable
fun EventsAdminListScreen(
    viewModel: EventsAdminListViewModel,
    onBack: () -> Unit,
    onOpenEvent: (Int) -> Unit,
    onNewEvent: () -> Unit,
    onNavigationEvent: (EventsAdminEvent) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Back from the form the list shows what was saved or deleted.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EventsAdminEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> onNavigationEvent(event)
            }
        }
    }

    BaseScreen(
        tabName = "Eventos",
        showBackArrow = true,
        onBackClick = onBack,
        // The green behind the sheet's rounded corners continues the tabs.
        containerColor = ipbGreen,
        topBarExtension = {
            GreenTabBar(
                tabs = FILTERS.map { GreenTab(it.label) },
                selectedIndex = FILTERS.indexOf(state.filter),
                onSelect = { viewModel.onFilterSelected(FILTERS[it]) },
            )
        },
    ) { innerPadding ->
        Box(
            Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .clip(RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius))
                .background(MaterialTheme.colorScheme.surfaceDim)
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            EventsAdminListContent(
                state = state,
                imageLoader = viewModel.imageLoader,
                onOpenEvent = onOpenEvent,
                onRetry = viewModel::load,
            )
            if (state.canCreate) {
                ExtendedFloatingActionButton(
                    onClick = onNewEvent,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Novo evento") },
                    containerColor = ipbGreen,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                )
            }
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        }
    }
}

private val FILTERS = listOf(EventStatusFilter.ACTIVE, EventStatusFilter.PAST, EventStatusFilter.ALL)
private val SheetRadius = 18.dp

/** The filter tabs live in the top bar; this is the sheet below them. */
@Composable
fun EventsAdminListContent(
    state: EventsAdminListUiState,
    imageLoader: ImageLoader,
    onOpenEvent: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        when {
            state.isLoading && state.items.isEmpty() ->
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.error != null && state.items.isEmpty() -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            ) {
                Text(state.error, textAlign = TextAlign.Center)
                Button(onClick = onRetry) { Text("Tentar novamente") }
            }
            state.items.isEmpty() -> Text(
                text = "Nenhum evento.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
            else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp)) {
                state.months.forEach { month ->
                    item(key = "month-${month.label}") { MonthHeader(month.label) }
                    item(key = "items-${month.label}") {
                        EventMonthCard(month = month, imageLoader = imageLoader, onOpenEvent = onOpenEvent)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun EventMonthCard(month: EventMonthUi, imageLoader: ImageLoader, onOpenEvent: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        month.items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA))
            }
            EventAdminRow(item = item, imageLoader = imageLoader, onClick = { onOpenEvent(item.id) })
        }
    }
}

private const val DIVIDER_ALPHA = 0.6f

@Preview(showBackground = true)
@Composable
private fun EventsAdminListContentPreview() {
    EventsAdminListContent(
        state = EventsAdminListUiState(
            isLoading = false,
            items = listOf(
                EventAdminItemUi(
                    1, "Congresso de Jovens", "14 a 16 nov · 19:30", "", false, false, "por Ana Paula",
                    "Novembro de 2026", "NOV", "14", "14 a 16 nov · 19:30",
                ),
                EventAdminItemUi(
                    2, "Assembleia", "30 nov", "", true, false, null, "Novembro de 2026", "NOV", "30", "10:00",
                ),
                EventAdminItemUi(
                    3, "Almoço de Páscoa", "5 abr", "", false, true, null, "Abril de 2026", "ABR", "05", "Dia todo",
                ),
            ),
            canCreate = true,
        ),
        imageLoader = ImageLoader(LocalContext.current),
        onOpenEvent = {},
        onRetry = {},
    )
}
