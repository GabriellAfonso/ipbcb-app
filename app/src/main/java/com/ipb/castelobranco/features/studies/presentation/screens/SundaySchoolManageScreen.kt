package com.ipb.castelobranco.features.studies.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.PanelAction
import com.ipb.castelobranco.core.presentation.components.PanelHeader
import com.ipb.castelobranco.core.presentation.components.TopActionPanel
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
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

private val SheetRadius = 18.dp
private val CardRadius = 16.dp
private val DraftBackground = Color(0xFFFDE6B3)
private val DraftText = Color(0xFF6B4300)
private const val BAND_SECONDARY_ALPHA = 0.8f
private const val CHIP_BORDER_ALPHA = 0.3f

/** What the actions panel acts on: the top bar's menu or one lesson. */
private sealed interface ManagePanel {
    data object Years : ManagePanel
    data class Lesson(val index: Int) : ManagePanel
}

@Composable
fun SundaySchoolManageScreen(
    viewModel: SundaySchoolManageViewModel,
    onBackClick: () -> Unit,
    onOpenLesson: (lessonId: Int?, year: Int) -> Unit,
    onOpenYears: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var panel by remember { mutableStateOf<ManagePanel?>(null) }
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
    val openLesson: (LessonSummary) -> Unit = { lesson -> state.selectedYear?.let { onOpenLesson(lesson.id, it) } }
    BaseScreen(
        tabName = "EBD",
        showBackArrow = true,
        onBackClick = onBackClick,
        // The green behind the sheet's rounded corners continues the band.
        containerColor = ipbGreen,
        extraActions = {
            IconButton(onClick = { panel = ManagePanel.Years }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções", tint = Color.White)
            }
        },
        topBarExtension = { YearBand(state, onSelectYear = viewModel::selectYear) },
    ) { innerPadding ->
        Box(
            Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .clip(RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius))
                .background(MaterialTheme.colorScheme.surfaceDim)
                .padding(bottom = innerPadding.calculateBottomPadding())
                .fillMaxSize(),
        ) {
            SundaySchoolManageContent(
                state = state,
                onOpenLesson = openLesson,
                onOpenActions = { panel = ManagePanel.Lesson(it) },
                onMove = viewModel::move,
                onDragStopped = viewModel::saveOrder,
                onRetry = viewModel::reload,
            )
            val year = state.selectedYear
            if (state.canManage && year != null) {
                ExtendedFloatingActionButton(
                    onClick = { onOpenLesson(null, year) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nova aula") },
                    containerColor = ipbGreen,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
            ManageActionsPanel(
                panel = panel,
                state = state,
                onDismiss = { panel = null },
                onOpenYears = onOpenYears,
                onOpenLesson = openLesson,
                onDelete = viewModel::askDelete,
            )
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
private fun ManageActionsPanel(
    panel: ManagePanel?,
    state: SundaySchoolManageUiState,
    onDismiss: () -> Unit,
    onOpenYears: () -> Unit,
    onOpenLesson: (LessonSummary) -> Unit,
    onDelete: (LessonSummary) -> Unit,
) {
    val lessonIndex = (panel as? ManagePanel.Lesson)?.index
    val lesson = lessonIndex?.let { state.lessons.getOrNull(it) }
    val actions = when {
        panel == ManagePanel.Years -> listOf(
            PanelAction(
                icon = Icons.Filled.CalendarMonth,
                label = "Anos e temas",
                description = "Dê ou troque o tema de cada ano",
                onClick = onOpenYears,
            ),
        )
        lesson != null -> listOfNotNull(
            PanelAction(icon = Icons.Filled.Edit, label = "Editar aula", onClick = { onOpenLesson(lesson) }),
            PanelAction(
                icon = Icons.Filled.DeleteOutline,
                label = "Apagar aula",
                description = "Não dá para desfazer",
                isDanger = true,
                onClick = { onDelete(lesson) },
            ).takeIf { state.canDelete },
        )
        else -> emptyList()
    }
    TopActionPanel(
        visible = actions.isNotEmpty(),
        actions = actions,
        onDismiss = onDismiss,
        header = lesson?.let {
            val status = if (it.status == LessonStatus.DRAFT) "Rascunho" else "Publicada"
            PanelHeader(
                name = "${lessonIndex + 1}. ${it.title}",
                detail = listOfNotNull(status, lessonMeta(it.date, it.teacher)).joinToString(" · "),
            )
        },
    )
}

/** Theme, counts and the years as chips, inside the green top-bar block. */
@Composable
private fun YearBand(state: SundaySchoolManageUiState, onSelectYear: (Int) -> Unit) {
    val year = state.selectedYear ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ipbGreen)
            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 16.dp),
    ) {
        Text(
            text = "Escola Bíblica Dominical",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = BAND_SECONDARY_ALPHA),
        )
        Text(
            text = state.theme.ifBlank { "Sem tema" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 2.dp),
        )
        Text(
            text = countsLabel(year, state),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = BAND_SECONDARY_ALPHA),
        )
        if (state.years.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .horizontalScroll(rememberScrollState()),
            ) {
                state.years.forEach { YearChip(it, selected = it == year, onClick = { onSelectYear(it) }) }
            }
        }
    }
}

@Composable
private fun YearChip(year: Int, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        text = year.toString(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (selected) ipbGreen else Color.White,
        modifier = Modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(Color.White)
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = CHIP_BORDER_ALPHA), shape)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** "2026 · 6 aulas · 2 rascunhos"; the drafts only when there are any. */
private fun countsLabel(year: Int, state: SundaySchoolManageUiState): String {
    val lessons = state.lessons.size.let { if (it == 1) "1 aula" else "$it aulas" }
    val drafts = state.draftCount.takeIf { it > 0 }?.let { if (it == 1) "1 rascunho" else "$it rascunhos" }
    return listOfNotNull(year.toString(), lessons, drafts).joinToString(" · ")
}

@Composable
fun SundaySchoolManageContent(
    state: SundaySchoolManageUiState,
    onOpenLesson: (LessonSummary) -> Unit,
    onOpenActions: (index: Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDragStopped: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        state.lessons.isNotEmpty() -> LessonsList(state, onOpenLesson, onOpenActions, onMove, onDragStopped)
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

/** Long-press the handle and drag; the new order is saved when the drag ends (FR-022). */
@Composable
private fun LessonsList(
    state: SundaySchoolManageUiState,
    onOpenLesson: (LessonSummary) -> Unit,
    onOpenActions: (index: Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDragStopped: () -> Unit,
) {
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to -> onMove(from.index, to.index) }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.lessons, key = { it.id }) { lesson ->
            ReorderableItem(reorderState, key = lesson.id) {
                val index = state.lessons.indexOf(lesson)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CardRadius))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable { onOpenLesson(lesson) }
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.canManage && !state.isSavingOrder) {
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .size(32.dp)
                                .longPressDraggableHandle(onDragStopped = { onDragStopped() }),
                        ) {
                            Icon(
                                Icons.Filled.DragHandle,
                                contentDescription = "Arrastar para reordenar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Box(Modifier.size(4.dp))
                    }
                    LessonNumber(index + 1)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (lesson.status == LessonStatus.DRAFT) DraftTag()
                            Text(
                                text = lessonMeta(lesson.date, lesson.teacher) ?: "Sem data",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(onClick = { onOpenActions(index) }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Opções da aula",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonNumber(number: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun DraftTag() {
    Text(
        text = "Rascunho",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = DraftText,
        modifier = Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(DraftBackground)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
