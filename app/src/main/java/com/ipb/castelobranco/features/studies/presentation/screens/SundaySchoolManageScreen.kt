package com.ipb.castelobranco.features.studies.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.studies.domain.model.LessonStatus
import com.ipb.castelobranco.features.studies.domain.model.LessonSummary
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineErrorView
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineLoadingView
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.lessonMeta
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolManageEvent
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolManageUiState
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolManageViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun SundaySchoolManageScreen(
    viewModel: SundaySchoolManageViewModel,
    onBackClick: () -> Unit,
    onOpenLesson: (lessonId: Int?, year: Int) -> Unit,
    onOpenYears: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LifecycleResumeEffect(viewModel) {
        viewModel.reload()
        onPauseOrDispose {}
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SundaySchoolManageEvent.ShowMessage -> snackbar.showSnackbar(event.text)
            }
        }
    }
    BaseScreen(
        tabName = "EBD",
        showBackArrow = true,
        onBackClick = onBackClick,
        extraActions = { ManageMenu(onOpenYears) },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            SundaySchoolManageContent(
                state = state,
                onSelectYear = viewModel::selectYear,
                onOpenLesson = { onOpenLesson(it.id, state.selectedYear ?: return@SundaySchoolManageContent) },
                onMove = viewModel::move,
                onDragStopped = viewModel::saveOrder,
                onDelete = viewModel::askDelete,
                onRetry = viewModel::reload,
            )
            val year = state.selectedYear
            if (state.canManage && year != null) {
                ExtendedFloatingActionButton(
                    onClick = { onOpenLesson(null, year) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nova aula") },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    state.pendingDelete?.let { lesson ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Apagar a aula “${lesson.title}”?") },
            text = { Text("Ela some do app para todos. Não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Apagar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissDelete) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ManageMenu(onOpenYears: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Anos da EBD") },
                onClick = {
                    expanded = false
                    onOpenYears()
                },
            )
        }
    }
}

@Composable
fun SundaySchoolManageContent(
    state: SundaySchoolManageUiState,
    onSelectYear: (Int) -> Unit,
    onOpenLesson: (LessonSummary) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDragStopped: () -> Unit,
    onDelete: (LessonSummary) -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        YearSelector(years = state.years, selected = state.selectedYear, onSelect = onSelectYear)
        when {
            state.lessons.isNotEmpty() -> LessonsList(state, onOpenLesson, onMove, onDragStopped, onDelete)
            state.isLoading -> DoctrineLoadingView()
            state.error != null -> DoctrineErrorView(message = state.error, onRetry = onRetry)
            else -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nenhuma aula neste ano",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun YearSelector(years: List<Int>, selected: Int?, onSelect: (Int) -> Unit) {
    if (selected == null) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedButton(onClick = { expanded = true }) {
            Text("Ano $selected")
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year.toString()) },
                    onClick = {
                        expanded = false
                        onSelect(year)
                    },
                )
            }
        }
    }
}

/** Long-press the handle and drag; the new order is saved when the drag ends (FR-022). */
@Composable
private fun LessonsList(
    state: SundaySchoolManageUiState,
    onOpenLesson: (LessonSummary) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDragStopped: () -> Unit,
    onDelete: (LessonSummary) -> Unit,
) {
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to -> onMove(from.index, to.index) }
    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 96.dp), modifier = Modifier.fillMaxSize()) {
        items(state.lessons, key = { it.id }) { lesson ->
            ReorderableItem(reorderState, key = lesson.id) {
                Surface(onClick = { onOpenLesson(lesson) }, color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.canManage && !state.isSavingOrder) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.longPressDraggableHandle(onDragStopped = { onDragStopped() }),
                            ) {
                                Icon(Icons.Filled.DragHandle, contentDescription = "Arrastar para reordenar")
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                            val meta = listOfNotNull(
                                "Rascunho".takeIf { lesson.status == LessonStatus.DRAFT },
                                lessonMeta(lesson.date, lesson.teacher),
                            ).joinToString(" · ")
                            if (meta.isNotBlank()) {
                                Text(
                                    text = meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (lesson.status == LessonStatus.DRAFT) FontWeight.SemiBold else null,
                                    color = if (lesson.status == LessonStatus.DRAFT) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                        if (state.canDelete) {
                            IconButton(onClick = { onDelete(lesson) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Apagar aula")
                            }
                        }
                    }
                }
            }
        }
    }
}
