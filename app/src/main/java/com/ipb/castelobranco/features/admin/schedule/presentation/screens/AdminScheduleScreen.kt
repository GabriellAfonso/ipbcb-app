package com.ipb.castelobranco.features.admin.schedule.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.admin.panel.presentation.navigation.AdminNav
import com.ipb.castelobranco.features.admin.schedule.domain.model.Member
import com.ipb.castelobranco.features.admin.schedule.presentation.components.ScheduleEditorTable
import com.ipb.castelobranco.features.admin.schedule.presentation.state.AdminScheduleEvent
import com.ipb.castelobranco.features.admin.schedule.presentation.state.AdminScheduleUiState
import com.ipb.castelobranco.features.admin.schedule.presentation.state.SaveResult
import com.ipb.castelobranco.features.admin.schedule.presentation.viewmodel.AdminScheduleViewModel
import com.ipb.castelobranco.features.schedule.domain.formatter.MonthScheduleWhatsappFormatter

private val Green = BrandColors.Green
private val Orange = BrandColors.Orange

// ── Actions ───────────────────────────────────────────────────────────────────

data class AdminScheduleActions(
    val onPreviousMonth: () -> Unit,
    val onNextMonth: () -> Unit,
    val onMemberSelect: (itemIndex: Int, member: Member) -> Unit,
    val onGenerate: () -> Unit,
    val onSave: () -> Unit,
    val onConfirmSave: () -> Unit,
    val onDismissSaveConfirmation: () -> Unit,
    val onShare: (String) -> Unit,
    val onDismissSaveResult: () -> Unit,
)

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun AdminScheduleScreen(
    nav: AdminNav,
    onShare: (String) -> Unit,
    viewModel: AdminScheduleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onEvent(AdminScheduleEvent.LoadMembers) }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.onEvent(AdminScheduleEvent.SnackbarShown)
    }

    val actions = AdminScheduleActions(
        onPreviousMonth = {
            val prev = if (state.month == 1) Pair(state.year - 1, 12)
                       else Pair(state.year, state.month - 1)
            viewModel.onEvent(AdminScheduleEvent.MonthChanged(prev.first, prev.second))
        },
        onNextMonth = {
            val next = if (state.month == 12) Pair(state.year + 1, 1)
                       else Pair(state.year, state.month + 1)
            viewModel.onEvent(AdminScheduleEvent.MonthChanged(next.first, next.second))
        },
        onMemberSelect = { index, member ->
            viewModel.onEvent(AdminScheduleEvent.MemberSelected(index, member))
        },
        onGenerate = { viewModel.onEvent(AdminScheduleEvent.GenerateSchedule) },
        onSave = { viewModel.onEvent(AdminScheduleEvent.SaveRequested) },
        onConfirmSave = { viewModel.onEvent(AdminScheduleEvent.SaveSchedule) },
        onDismissSaveConfirmation = { viewModel.onEvent(AdminScheduleEvent.SaveConfirmationDismissed) },
        onShare = onShare,
        onDismissSaveResult = { viewModel.onEvent(AdminScheduleEvent.SaveResultDismissed) },
    )

    AdminScheduleContent(
        state = state,
        actions = actions,
        snackbarHostState = snackbarHostState,
        onBackClick = nav.back
    )
}

// ── Stateless screen ──────────────────────────────────────────────────────────

@Composable
fun AdminScheduleContent(
    state: AdminScheduleUiState,
    actions: AdminScheduleActions,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit
) {
    BaseScreen(
        tabName = "Escala",
        logoRes = R.drawable.ic_schedule,
        showBackArrow = true,
        onBackClick = onBackClick
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScreenTitle()

                Spacer(modifier = Modifier.height(16.dp))

                MonthSelector(
                    monthLabel = state.monthLabel,
                    onPrevious = actions.onPreviousMonth,
                    onNext = actions.onNextMonth
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    ScheduleEditorTable(
                        items = state.items,
                        members = state.members,
                        onMemberSelect = actions.onMemberSelect
                    )
                    if (state.isGenerating) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            CircularProgressIndicator(color = Green)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                ActionButtons(
                    isGenerating = state.isGenerating,
                    isSaving = state.isSaving,
                    canSave = state.canSave,
                    canShare = state.canShare,
                    onGenerate = actions.onGenerate,
                    onSave = actions.onSave,
                    onShare = {
                        val text = state.toWhatsappText()
                        actions.onShare(text)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            if (state.showSaveConfirmation) {
                SaveConfirmationDialog(
                    monthLabel = state.monthLabel,
                    onConfirm = actions.onConfirmSave,
                    onDismiss = actions.onDismissSaveConfirmation
                )
            }

            SaveResultDialog(
                result = state.saveResult,
                onShare = {
                    actions.onShare(state.toWhatsappText())
                    actions.onDismissSaveResult()
                },
                onDismiss = actions.onDismissSaveResult
            )
        }
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun ScreenTitle() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .height(3.dp)
                .fillMaxWidth(0.12f)
                .background(Orange, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Gerar Escala",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Green
        )
    }
}

@Composable
private fun MonthSelector(
    monthLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowLeft,
                contentDescription = "Mês anterior",
                tint = Green,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = "Próximo mês",
                tint = Green,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ActionButtons(
    isGenerating: Boolean,
    isSaving: Boolean,
    canSave: Boolean,
    canShare: Boolean,
    onGenerate: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Gerar aleatória
        OutlinedButton(
            onClick = onGenerate,
            enabled = !isGenerating && !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            border = BorderStroke(1.dp, Green.copy(alpha = 0.5f))
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Green,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isGenerating) "Gerando..." else "Gerar aleatória",
                fontWeight = FontWeight.SemiBold,
                color = Green
            )
        }

        // Salvar escala
        Button(
            onClick = onSave,
            enabled = canSave && !isSaving && !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green,
                disabledContainerColor = Green.copy(alpha = 0.4f)
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isSaving) "Salvando..." else "Salvar escala",
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        // Compartilhar — só aparece após salvar com sucesso e enquanto não houver alterações
        if (canShare) {
            OutlinedButton(
                onClick = onShare,
                enabled = !isGenerating && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                border = BorderStroke(1.dp, Orange.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    tint = Orange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Compartilhar escala",
                    fontWeight = FontWeight.SemiBold,
                    color = Orange
                )
            }
        }
    }
}

@Composable
private fun SaveConfirmationDialog(
    monthLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Salvar escala de $monthLabel?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Salvar", fontWeight = FontWeight.SemiBold, color = Green)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun SaveResultDialog(
    result: SaveResult?,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (result) {
        is SaveResult.Success -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Escala salva") },
                text = { Text("Sua escala foi salva com sucesso.") },
                confirmButton = {
                    TextButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            tint = Orange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Compartilhar",
                            fontWeight = FontWeight.SemiBold,
                            color = Orange
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Fechar")
                    }
                }
            )
        }
        is SaveResult.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Erro ao salvar") },
                text = { Text(result.message) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            )
        }
        null -> Unit
    }
}

// ── Formatter helper ──────────────────────────────────────────────────────────

private fun AdminScheduleUiState.toWhatsappText(): String {
    val locale = java.util.Locale.forLanguageTag("pt-BR")
    val monthName = MonthScheduleWhatsappFormatter.monthPtBr(month).uppercase(locale)
    val order = listOf("terça", "terca", "quinta", "domingo")

    val grouped = items.groupBy { it.scheduleTypeName }
    val sortedGroups = grouped.entries.sortedWith(
        compareBy { entry ->
            val t = entry.key.trim().lowercase(locale)
            order.indexOfFirst { t.startsWith(it) }.let { if (it == -1) Int.MAX_VALUE else it }
        }
    )

    val sb = StringBuilder()
    sb.append("ESCALA DE $monthName $year - DIRIGENTES E RESPONSÁVEIS\n\n")

    sortedGroups.forEach { (typeName, groupItems) ->
        // time isn't stored in EditableScheduleUiState, use 19:30 as default (same as save)
        sb.append("$typeName (19:30)\n\n")
        groupItems.sortedBy { it.day }.forEach { item ->
            sb.append(String.format("%02d", item.day))
                .append("- ")
                .append(item.selectedMember?.name ?: "---")
                .append("\n")
        }
        sb.append("\n")
    }

    sb.append("*Cafezinho pós culto de Adoração (Ceia) todo 4° Domingo\n\n")
    sb.append("* Aberto a participação de qualquer irmão.\n\n")
    sb.append("DEUS ABENÇOE")

    return sb.toString().trim()
}
