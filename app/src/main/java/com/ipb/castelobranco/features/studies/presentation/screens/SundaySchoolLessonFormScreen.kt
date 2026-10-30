package com.ipb.castelobranco.features.studies.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.DateFieldWithPicker
import com.ipb.castelobranco.core.presentation.components.GreenTab
import com.ipb.castelobranco.core.presentation.components.GreenTabBar
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.studies.domain.model.BibleReference
import com.ipb.castelobranco.features.studies.domain.model.LessonDraft
import com.ipb.castelobranco.features.studies.domain.model.LessonStatus
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.DEFAULT_FONT_SIZE
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineErrorView
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineLoadingView
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.LessonContentView
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.LessonEditorActions
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.LessonHeaderInfo
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.LessonStyle
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.lessonEditor
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.sundaySchoolColors
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

    val hasDraft = state.draft != null
    BaseScreen(
        tabName = if (state.isNew) "Nova aula" else "Editar aula",
        showBackArrow = true,
        onBackClick = viewModel::onBack,
        // The green behind the sheet's rounded corners continues the tabs.
        containerColor = if (hasDraft) ipbGreen else MaterialTheme.colorScheme.background,
        topBarExtension = if (hasDraft) {
            {
                GreenTabBar(
                    tabs = listOf(GreenTab("Editar"), GreenTab("Pré-visualizar")),
                    selectedIndex = state.mode.ordinal,
                    onSelect = { viewModel.setMode(LessonFormMode.entries[it]) },
                )
            }
        } else {
            null
        },
    ) { innerPadding ->
        val contentModifier = if (hasDraft) {
            Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .clip(RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius))
                .background(MaterialTheme.colorScheme.surfaceDim)
                .padding(bottom = innerPadding.calculateBottomPadding())
        } else {
            Modifier.padding(innerPadding)
        }
        Column(contentModifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
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
            state.draft?.let { draft ->
                SaveBar(status = draft.status, isSaving = state.isSaving, onSave = viewModel::save)
            }
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

/** Fixed at the bottom; the label follows the chosen status. */
@Composable
private fun SaveBar(status: LessonStatus, isSaving: Boolean, onSave: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Button(
            onClick = onSave,
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = ipbGreen, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(50.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text(if (status == LessonStatus.PUBLISHED) "Salvar e publicar" else "Salvar rascunho")
            }
        }
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
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item(key = "metadata") { MetadataFields(draft, state, formActions) }
                item(key = "content-title") {
                    FormSectionTitle("Conteúdo", Modifier.padding(start = 16.dp, top = 8.dp))
                }
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

/** "Aula": the lesson's data in a card with tonal fields and the status selector. */
@Composable
private fun MetadataFields(draft: LessonDraft, state: SundaySchoolLessonFormUiState, actions: LessonFormActions) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FormSectionTitle("Aula")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CardRadius))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.formErrors.forEach {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            TonalField(
                value = draft.title,
                onValueChange = actions.onTitle,
                label = "Título",
                error = state.fieldErrors[FIELD_TITLE],
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TonalField(
                    value = if (draft.year == 0) "" else draft.year.toString(),
                    onValueChange = actions.onYear,
                    label = "Ano",
                    error = state.fieldErrors[FIELD_YEAR],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                TonalField(
                    value = draft.teacher,
                    onValueChange = actions.onTeacher,
                    label = "Professor (opcional)",
                    error = state.fieldErrors[FIELD_TEACHER],
                    modifier = Modifier.weight(2f),
                )
            }
            LessonDateField(date = draft.date, error = state.fieldErrors[FIELD_DATE], onChange = actions.onDate)
            StatusSelector(selected = draft.status, onSelect = actions.onStatus)
        }
    }
}

@Composable
private fun FormSectionTitle(title: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 4.dp),
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 15.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TonalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            errorContainerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = modifier,
    )
}

/** Rascunho / Publicada as a two-way pill; Publicada lights up green. */
@Composable
private fun StatusSelector(selected: LessonStatus, onSelect: (LessonStatus) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        LessonStatus.entries.forEach { status ->
            val isSelected = status == selected
            val background = when {
                !isSelected -> Color.Transparent
                status == LessonStatus.PUBLISHED -> ipbGreen
                else -> MaterialTheme.colorScheme.surface
            }
            val content = when {
                !isSelected -> MaterialTheme.colorScheme.onSurfaceVariant
                status == LessonStatus.PUBLISHED -> Color.White
                else -> MaterialTheme.colorScheme.onSurface
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(background)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(status) })
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = if (status == LessonStatus.DRAFT) "Rascunho" else "Publicada",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
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
private val SheetRadius = 18.dp
private val CardRadius = 18.dp
