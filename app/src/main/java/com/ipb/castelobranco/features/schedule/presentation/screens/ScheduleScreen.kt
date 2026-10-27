package com.ipb.castelobranco.features.schedule.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.ElasticPullToRefresh
import com.ipb.castelobranco.core.presentation.components.GreenTab
import com.ipb.castelobranco.core.presentation.components.GreenTabBar
import com.ipb.castelobranco.core.presentation.components.PermissionErrorPlaceholder
import com.ipb.castelobranco.core.presentation.components.access.MembersOnlyPlaceholder
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.schedule.domain.formatter.MonthScheduleWhatsappFormatter
import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleTabContent
import com.ipb.castelobranco.features.schedule.presentation.viewmodel.ScheduleUiState
import com.ipb.castelobranco.features.schedule.presentation.viewmodel.ScheduleViewModel
import java.util.Locale

private const val HTTP_FORBIDDEN = 403
private val SheetRadius = 18.dp
private val PT_BR = Locale.forLanguageTag("pt-BR")

@Composable
fun MonthScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onShare: (String) -> Unit,
    onNavigateToAuth: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    MonthScheduleContent(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshMonthSchedule() },
        onShare = onShare,
        onBackClick = onBackClick,
        onNavigateToAuth = onNavigateToAuth,
    )
}

@Composable
fun MonthScheduleContent(
    uiState: ScheduleUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onShare: (String) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToAuth: () -> Unit = {},
) {
    val success = uiState as? ScheduleUiState.Success
    // Keyed by the next service so the screen opens on it; saveable so rotation keeps the user's pick.
    var selectedTab by rememberSaveable(success?.initialTabIndex) { mutableIntStateOf(success?.initialTabIndex ?: 0) }
    val tabIndex = success?.let { selectedTab.coerceIn(0, it.sections.lastIndex) } ?: 0

    BaseScreen(
        tabName = "Escala",
        logoRes = R.drawable.ic_schedule,
        showBackArrow = true,
        onBackClick = onBackClick,
        // The green behind the sheet's rounded corners continues the top bar.
        containerColor = if (success != null) ipbGreen else MaterialTheme.colorScheme.background,
        extraActions = {
            if (success != null) {
                IconButton(onClick = { onShare(MonthScheduleWhatsappFormatter.format(success.data)) }) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = "Compartilhar", tint = Color.White)
                }
            }
        },
        topBarExtension = success?.let { state ->
            {
                GreenTabBar(
                    tabs = state.sections.map { GreenTab(it.shortTitle, it.time) },
                    selectedIndex = tabIndex,
                    onSelect = { selectedTab = it },
                )
            }
        },
    ) { innerPadding ->
        val contentModifier = if (success != null) {
            Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .clip(RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius))
                .background(MaterialTheme.colorScheme.surfaceDim)
                .padding(bottom = innerPadding.calculateBottomPadding())
        } else {
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        }

        ElasticPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = contentModifier,
        ) {
            when (uiState) {
                is ScheduleUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is ScheduleUiState.Empty -> {
                    Text(
                        text = "Nenhuma escala encontrada.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is ScheduleUiState.Success -> {
                    ScheduleTabContent(
                        section = uiState.sections[tabIndex],
                        nextDay = uiState.nextDays.getOrNull(tabIndex),
                        month = uiState.data.month,
                        monthName = MonthScheduleWhatsappFormatter.monthPtBr(uiState.data.month).lowercase(PT_BR),
                    )
                }

                is ScheduleUiState.Error -> if (uiState.httpCode == HTTP_FORBIDDEN) {
                    MembersOnlyPlaceholder(message = uiState.message, modifier = Modifier.align(Alignment.Center))
                } else {
                    PermissionErrorPlaceholder(
                        message = uiState.message,
                        onLoginClick = onNavigateToAuth,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}
