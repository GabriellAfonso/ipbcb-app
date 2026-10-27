package com.ipb.castelobranco.features.studies.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolYearAdmin
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineErrorView
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineLoadingView
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolYearsUiState
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolYearsViewModel

@Composable
fun SundaySchoolYearsScreen(
    viewModel: SundaySchoolYearsViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    BaseScreen(tabName = "Anos da EBD", showBackArrow = true, onBackClick = onBackClick) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            SundaySchoolYearsContent(state = state, onEdit = viewModel::edit, onRetry = viewModel::load)
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
    state.editing?.let { year ->
        ThemeDialog(
            year = year,
            isSaving = state.isSaving,
            onSave = viewModel::saveTheme,
            onDismiss = viewModel::dismissEdit,
        )
    }
}

@Composable
fun SundaySchoolYearsContent(
    state: SundaySchoolYearsUiState,
    onEdit: (SundaySchoolYearAdmin) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        state.years.isNotEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
            items(state.years, key = { it.year }) { year ->
                ListItem(
                    headlineContent = { Text(year.year.toString(), style = MaterialTheme.typography.titleMedium) },
                    supportingContent = {
                        Column {
                            Text(year.theme.ifBlank { "Sem tema" })
                            Text(
                                text = countsLabel(year),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.canManage) { onEdit(year) },
                )
                HorizontalDivider()
            }
        }
        state.isLoading -> DoctrineLoadingView()
        state.error != null -> DoctrineErrorView(message = state.error, onRetry = onRetry)
        else -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Os anos aparecem quando a primeira aula deles é criada.")
        }
    }
}

private fun countsLabel(year: SundaySchoolYearAdmin): String {
    val lessons = if (year.lessonCount == 1) "1 aula" else "${year.lessonCount} aulas"
    val published = if (year.publishedCount == 1) "1 publicada" else "${year.publishedCount} publicadas"
    return "$lessons · $published"
}

@Composable
private fun ThemeDialog(
    year: SundaySchoolYearAdmin,
    isSaving: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var theme by rememberSaveable(year.year) { mutableStateOf(year.theme) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tema de ${year.year}") },
        text = {
            OutlinedTextField(
                value = theme,
                onValueChange = { theme = it.take(MAX_THEME) },
                label = { Text("Tema") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(theme) }, enabled = !isSaving) { Text("Salvar") } },
        dismissButton = {
            if (year.theme.isNotBlank()) {
                TextButton(onClick = { onSave("") }, enabled = !isSaving) { Text("Remover tema") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}

private const val MAX_THEME = 200
