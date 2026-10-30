package com.ipb.castelobranco.features.studies.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        Box(Modifier.padding(innerPadding).fillMaxSize().background(MaterialTheme.colorScheme.surfaceDim)) {
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
        state.years.isNotEmpty() -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.years, key = { it.year }) { year ->
                YearCard(year, enabled = state.canManage, onClick = { onEdit(year) })
            }
            item(key = "note") {
                Text(
                    text = "Um ano aparece aqui quando a primeira aula dele é criada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                )
            }
        }
        state.isLoading -> DoctrineLoadingView()
        state.error != null -> DoctrineErrorView(message = state.error, onRetry = onRetry)
        else -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Os anos aparecem quando a primeira aula deles é criada.")
        }
    }
}

@Composable
private fun YearCard(year: SundaySchoolYearAdmin, enabled: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
    ) {
        Text(
            text = year.year.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(min = 62.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            if (year.theme.isBlank()) {
                Text(
                    text = "Sem tema",
                    style = MaterialTheme.typography.titleSmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(year.theme, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = countsLabel(year),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (enabled) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
