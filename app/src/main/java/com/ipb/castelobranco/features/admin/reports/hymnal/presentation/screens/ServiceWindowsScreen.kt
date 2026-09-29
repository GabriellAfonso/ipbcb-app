package com.ipb.castelobranco.features.admin.reports.hymnal.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ServiceWindow
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components.ServiceWindowFormDialog
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components.ServiceWindowRow
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.state.ServiceWindowsEvent
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.state.ServiceWindowsUiState
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.viewmodel.ServiceWindowsViewModel
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * The weekly services the report groups by.
 *
 * Nothing on this screen deletes history: occurrences are derived at read time, so removing a
 * service only changes how the next report reads the same events.
 */
@Composable
fun ServiceWindowsScreen(
    viewModel: ServiceWindowsViewModel,
    onBack: () -> Unit,
    onWindowsChanged: () -> Unit,
    onLeaveArea: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ServiceWindowsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                ServiceWindowsEvent.WindowsChanged -> onWindowsChanged()
                is ServiceWindowsEvent.LeaveArea -> onLeaveArea(event.message)
            }
        }
    }

    BaseScreen(
        tabName = "Janelas de culto",
        logoRes = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick = onBack,
        extraActions = {
            if (state.canManage) {
                IconButton(onClick = viewModel::onCreateRequested) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Novo culto")
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            ServiceWindowsContent(
                state = state,
                innerPadding = innerPadding,
                onEdit = viewModel::onEditRequested,
                onToggleActive = viewModel::onActiveToggled,
                onDelete = viewModel::onDeleteRequested,
                onRetry = viewModel::load,
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    val editing = state.editing
    if (editing != null) {
        ServiceWindowFormDialog(
            draft = editing,
            fieldErrors = state.fieldErrors,
            isSaving = state.isSaving,
            onDraftChanged = viewModel::onDraftChanged,
            onDismiss = viewModel::onEditDismissed,
            onSave = viewModel::onSave,
        )
    }

    val pendingDelete = state.pendingDelete
    if (pendingDelete != null) {
        DeleteWindowDialog(
            window = pendingDelete,
            onDismiss = viewModel::onDeleteDismissed,
            onConfirm = viewModel::onDeleteConfirmed,
        )
    }
}

@Composable
fun ServiceWindowsContent(
    state: ServiceWindowsUiState,
    innerPadding: PaddingValues,
    onEdit: (ServiceWindow) -> Unit,
    onToggleActive: (ServiceWindow) -> Unit,
    onDelete: (ServiceWindow) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        state.isLoading -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        state.error != null -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = state.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text("Tentar novamente") }
        }

        state.windows.isEmpty() -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
        ) {
            Text(
                text = "Nenhum culto cadastrado",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sem janelas de culto, todas as ocorrências se agrupam por dia de " +
                    "calendário. Cadastre os cultos para que os relatórios saibam separá-los.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }

        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.windows, key = { it.id }) { window ->
                ServiceWindowRow(
                    window = window,
                    editable = state.canManage,
                    onEdit = { onEdit(window) },
                    onToggleActive = { onToggleActive(window) },
                    onDelete = { onDelete(window) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * The confirmation states plainly what deleting does and does not do, because the intuition —
 * "apagar o culto apaga o histórico dele" — is wrong and expensive.
 */
@Composable
private fun DeleteWindowDialog(
    window: ServiceWindow,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apagar \"${window.name}\"?") },
        text = {
            Text(
                "Nenhum registro do histórico é apagado. As ocorrências que estavam agrupadas " +
                    "neste culto passam a se agrupar por dia de calendário.\n\n" +
                    "Se o culto apenas deixou de acontecer, prefira desativá-lo: ele continua na " +
                    "lista e pode voltar a valer quando quiser."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Apagar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun ServiceWindowsContentPreview() {
    ServiceWindowsContent(
        state = ServiceWindowsUiState(
            isLoading = false,
            canManage = true,
            windows = listOf(
                ServiceWindow(
                    id = 3,
                    name = "Culto de Domingo à Noite",
                    weekday = DayOfWeek.SUNDAY,
                    startTime = LocalTime.of(19, 0),
                    endTime = LocalTime.of(21, 0),
                    active = true,
                ),
                ServiceWindow(
                    id = 7,
                    name = "Culto de Oração",
                    weekday = DayOfWeek.WEDNESDAY,
                    startTime = LocalTime.of(19, 30),
                    endTime = LocalTime.of(21, 0),
                    active = false,
                ),
            ),
        ),
        innerPadding = PaddingValues(0.dp),
        onEdit = {},
        onToggleActive = {},
        onDelete = {},
        onRetry = {},
    )
}
