package com.ipb.castelobranco.features.schedule.presentation.screens

    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.material3.CircularProgressIndicator
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Text
    import com.ipb.castelobranco.core.presentation.components.ElasticPullToRefresh
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.getValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
    import androidx.lifecycle.compose.collectAsStateWithLifecycle
    import com.ipb.castelobranco.R
    import com.ipb.castelobranco.core.presentation.base.BaseScreen
    import com.ipb.castelobranco.features.schedule.domain.formatter.MonthScheduleWhatsappFormatter
    import com.ipb.castelobranco.features.schedule.domain.model.MonthSchedule
    import com.ipb.castelobranco.features.schedule.presentation.components.MonthScheduleTable
    import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleSectionUi
    import com.ipb.castelobranco.core.presentation.components.PermissionErrorPlaceholder
    import com.ipb.castelobranco.core.presentation.components.access.MembersOnlyPlaceholder
    import com.ipb.castelobranco.features.schedule.presentation.viewmodel.ScheduleUiState
    import com.ipb.castelobranco.features.schedule.presentation.viewmodel.ScheduleViewModel

    // Removi o data class MonthScheduleUiState antigo, pois agora usamos o ScheduleUiState do ViewModel

    private const val HTTP_FORBIDDEN = 403

    data class MonthScheduleActions(
        val onShare: (String) -> Unit,
        val onGenerateNewSchedule: () -> Unit = {},
        val onRefresh: () -> Unit = {}
    )

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
        BaseScreen(
            tabName = "Escala",
            logoRes = R.drawable.ic_schedule,
            showBackArrow = true,
            onBackClick = onBackClick,
        ) { innerPadding ->

            ElasticPullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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
                        ScheduleContent(
                            sections = uiState.sections,
                            monthSchedule = uiState.data,
                            onShare = onShare
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

    @Composable
    private fun ScheduleContent(
        sections: List<ScheduleSectionUi>,
        monthSchedule: MonthSchedule,
        onShare: (String) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // 1. TÍTULO FIXO (Fora do scroll)
            Text(
                text = "Escala de ${MonthScheduleWhatsappFormatter.monthPtBr(monthSchedule.month)}",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // 2. ÁREA ROLÁVEL (Apenas a tabela)
            Box(
                modifier = Modifier
                    .weight(1f) // Ocupa todo o espaço entre o título e os botões
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                MonthScheduleTable(sections = sections)
            }

        }
    }
    // Mantenho esta função aqui, pois o seu ViewModel a importa deste pacote.
    // DICA: No futuro, mova isso para um arquivo "ScheduleUiMapper.kt" para limpar a View.
