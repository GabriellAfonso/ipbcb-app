package com.ipb.castelobranco.features.studies.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.DateFieldWithPicker
import com.ipb.castelobranco.features.studies.domain.model.BibleReference
import com.ipb.castelobranco.features.studies.domain.model.LessonDraft
import com.ipb.castelobranco.features.studies.domain.model.LessonStatus
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineErrorView
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineLoadingView
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.LessonContentView
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.LessonEditorActions
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.LessonHeaderInfo
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.LessonStyle
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.lessonEditor
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.sundaySchoolColors
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.DEFAULT_FONT_SIZE
import com.ipb.castelobranco.features.studies.presentation.viewmodel.LessonFormMode
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolLessonFormEvent
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolLessonFormUiState
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolLessonFormViewModel
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolLessonFormViewModel.Companion.FIELD_DATE
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolLessonFormViewModel.Companion.FIELD_TEACHER
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolLessonFormViewModel.Companion.FIELD_TITLE
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolLessonFormViewModel.Companion.FIELD_YEAR
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** What the form's metadata fields can change. */
data class LessonFormActions(
    val onTitle: (String) -> Unit,
    val onYear: (String) -> Unit,
    val onDate: (LocalDate?) -> Unit,
    val onTeacher: (String) -> Unit,
    val onStatus: (LessonStatus) -> Unit,
)

@Composable
fun SundaySchoolLessonFormScreen(
    viewModel: SundaySchoolLessonFormViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SundaySchoolLessonFormEvent.ShowMessage -> snackbar.showSnackbar(event.text)
                SundaySchoolLessonFormEvent.Close -> onClose()
            }
        }
    }
    BackHandler(onBack = viewModel::onBack)

    BaseScreen(
        tabName = if (state.isNew) "Nova aula" else "Editar aula",
        showBackArrow = true,
        onBackClick = viewModel::onBack,
        extraActions = {
            if (state.draft != null) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(12.dp).padding(end = 4.dp))
                } else {
                    IconButton(onClick = viewModel::save) { Icon(Icons.Filled.Check, contentDescription = "Salvar") }
                }
            }
        },
        topBarExtension = {
            if (state.draft != null) {
                PrimaryTabRow(selectedTabIndex = state.mode.ordinal) {
                    Tab(
                        selected = state.mode == LessonFormMode.EDIT,
                        onClick = { viewModel.setMode(LessonFormMode.EDIT) },
                        text = { Text("Editar") },
                    )
                    Tab(
                        selected = state.mode == LessonFormMode.PREVIEW,
                        onClick = { viewModel.setMode(LessonFormMode.PREVIEW) },
                        text = { Text("Pré-visualizar") },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            SundaySchoolLessonFormContent(
                state = state,
                formActions = LessonFormActions(
                    onTitle = viewModel::setTitle,
                    onYear = viewModel::setYear,
                    onDate = viewModel::setDate,
                    onTeacher = viewModel::setTeacher,
                    onStatus = viewModel::setStatus,
                ),
                editorActions = LessonEditorActions(
                    onSectionTitle = viewModel::setSectionTitle,
                    onSectionIcon = viewModel::setSectionIcon,
                    onMoveSection = viewModel::moveSection,
                    onDeleteSection = viewModel::deleteSection,
                    onAddSection = viewModel::addSection,
                    onAddBlock = viewModel::addBlock,
                    onUpdateBlock = viewModel::updateBlock,
                    onMoveBlock = viewModel::moveBlock,
                    onDeleteBlock = viewModel::deleteBlock,
                ),
                onRetry = viewModel::load,
            )
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (state.confirmDiscard) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDiscard,
            title = { Text("Descartar alterações?") },
            text = { Text("As mudanças desta aula ainda não foram salvas.") },
            confirmButton = { TextButton(onClick = viewModel::discard) { Text("Descartar") } },
            dismissButton = { TextButton(onClick = viewModel::dismissDiscard) { Text("Continuar editando") } },
        )
    }
}

@Composable
fun SundaySchoolLessonFormContent(
    state: SundaySchoolLessonFormUiState,
    formActions: LessonFormActions,
    editorActions: LessonEditorActions,
    onRetry: () -> Unit,
) {
    val draft = state.draft
    when {
        draft == null && state.isLoading -> DoctrineLoadingView()
        draft == null -> DoctrineErrorView(message = state.loadError.orEmpty(), onRetry = onRetry)
        state.mode == LessonFormMode.PREVIEW -> LessonPreview(state, draft)
        else -> {
            val style = LessonStyle(colors = sundaySchoolColors, fontSize = DEFAULT_FONT_SIZE)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                item(key = "metadata") { MetadataFields(draft, state, formActions) }
                lessonEditor(
                    content = draft.content,
                    books = state.books,
                    errors = state.locationErrors,
                    catalog = state.catalog,
                    style = style,
                    actions = editorActions,
                )
            }
        }
    }
}

/** Exactly the member reader (FR-020); references do nothing here. */
@Composable
private fun LessonPreview(state: SundaySchoolLessonFormUiState, draft: LessonDraft) {
    val colors = sundaySchoolColors
    Surface(modifier = Modifier.fillMaxSize(), color = colors.page, contentColor = colors.text) {
        LessonContentView(
            header = LessonHeaderInfo(
                title = draft.title,
                yearLabel = state.yearLabel.ifBlank { draft.year.toString() },
                date = draft.date,
                teacher = draft.teacher,
            ),
            content = draft.content,
            books = state.books,
            style = LessonStyle(colors = colors, fontSize = DEFAULT_FONT_SIZE),
            onReferenceClick = { _: BibleReference -> },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun MetadataFields(draft: LessonDraft, state: SundaySchoolLessonFormUiState, actions: LessonFormActions) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.formErrors.forEach {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedTextField(
            value = draft.title,
            onValueChange = actions.onTitle,
            label = { Text("Título") },
            isError = state.fieldErrors[FIELD_TITLE] != null,
            supportingText = state.fieldErrors[FIELD_TITLE]?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = if (draft.year == 0) "" else draft.year.toString(),
                onValueChange = actions.onYear,
                label = { Text("Ano") },
                isError = state.fieldErrors[FIELD_YEAR] != null,
                supportingText = state.fieldErrors[FIELD_YEAR]?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = draft.teacher,
                onValueChange = actions.onTeacher,
                label = { Text("Professor (opcional)") },
                isError = state.fieldErrors[FIELD_TEACHER] != null,
                supportingText = state.fieldErrors[FIELD_TEACHER]?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.weight(2f),
            )
        }
        LessonDateField(date = draft.date, error = state.fieldErrors[FIELD_DATE], onChange = actions.onDate)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Status", style = MaterialTheme.typography.labelLarge)
            LessonStatus.entries.forEach { status ->
                FilterChip(
                    selected = draft.status == status,
                    onClick = { actions.onStatus(status) },
                    label = { Text(if (status == LessonStatus.DRAFT) "Rascunho" else "Publicado") },
                )
            }
        }
    }
}

@Composable
private fun LessonDateField(date: LocalDate?, error: String?, onChange: (LocalDate?) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DateFieldWithPicker(
                dateBr = date?.format(DATE_FORMAT).orEmpty(),
                onOpenPicker = { picking = true },
                modifier = Modifier.weight(1f),
                placeholder = "Data (opcional)",
            )
            if (date != null) TextButton(onClick = { onChange(null) }) { Text("Limpar") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
    if (picking) {
        LessonDatePickerDialog(
            initial = date,
            onDismiss = { picking = false },
            onConfirm = {
                onChange(it)
                picking = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonDatePickerDialog(initial: LocalDate?, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                },
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) {
        DatePicker(state = state)
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
