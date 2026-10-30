package com.ipb.castelobranco.features.admin.events.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.HighlightEvent
import com.ipb.castelobranco.core.presentation.components.HighlightEventUi
import com.ipb.castelobranco.core.presentation.components.PanelAction
import com.ipb.castelobranco.core.presentation.components.PanelHeader
import com.ipb.castelobranco.core.presentation.components.PanelMoreButton
import com.ipb.castelobranco.core.presentation.components.TopActionPanel
import com.ipb.castelobranco.core.presentation.components.rememberCroppedPhotoPicker
import com.ipb.castelobranco.core.presentation.format.EventDateLabel
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.events.domain.model.EventDraft
import com.ipb.castelobranco.features.admin.events.domain.model.EventField
import com.ipb.castelobranco.features.admin.events.domain.model.EventVisibility
import com.ipb.castelobranco.features.admin.events.presentation.components.DeleteEventDialog
import com.ipb.castelobranco.features.admin.events.presentation.components.EventDateField
import com.ipb.castelobranco.features.admin.events.presentation.components.EventImageField
import com.ipb.castelobranco.features.admin.events.presentation.components.EventTextField
import com.ipb.castelobranco.features.admin.events.presentation.components.EventTimeField
import com.ipb.castelobranco.features.admin.events.presentation.components.VisibilitySelector
import com.ipb.castelobranco.features.admin.events.presentation.state.EventFormUiState
import com.ipb.castelobranco.features.admin.events.presentation.state.EventsAdminEvent
import com.ipb.castelobranco.features.admin.events.presentation.viewmodel.EventFormViewModel

private const val CROP_ASPECT_X = 3f
private const val CROP_ASPECT_Y = 2f
private const val CROP_MAX_WIDTH = 1800
private const val CROP_MAX_HEIGHT = 1200
private const val CROP_TITLE = "Recortar imagem"

@Composable
fun EventFormScreen(
    viewModel: EventFormViewModel,
    onBack: () -> Unit,
    onEvent: (EventsAdminEvent) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showPanel by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(EventFormTab.EDIT) }
    val pickImage = rememberCroppedPhotoPicker(
        aspectX = CROP_ASPECT_X,
        aspectY = CROP_ASPECT_Y,
        maxWidth = CROP_MAX_WIDTH,
        maxHeight = CROP_MAX_HEIGHT,
        title = CROP_TITLE,
        onPicked = viewModel::onImagePicked,
    )
    val leave = { if (state.hasUnsavedChanges) confirmDiscard = true else onBack() }

    BackHandler(enabled = state.hasUnsavedChanges) { confirmDiscard = true }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EventsAdminEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> onEvent(event)
            }
        }
    }

    BaseScreen(
        tabName = when {
            !state.isEditing -> "Novo evento"
            state.isReadOnly -> "Evento"
            else -> "Editar evento"
        },
        showBackArrow = true,
        onBackClick = leave,
        extraActions = {
            PanelMoreButton(hasActions = state.canDelete && !state.isLoading) { showPanel = true }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            EventFormContent(
                state = state,
                tab = tab,
                imageLoader = viewModel.imageLoader,
                onSelectTab = { tab = it },
                onPickImage = pickImage,
                onDraftChanged = viewModel::onDraftChanged,
                // Field errors show on "Editar": saving from the preview brings it back.
                onSave = {
                    tab = EventFormTab.EDIT
                    viewModel.onSave()
                },
                onRetry = viewModel::load,
            )
            TopActionPanel(
                visible = showPanel,
                header = PanelHeader(name = state.draft.title, detail = state.previewDateLabel),
                actions = listOf(
                    PanelAction(
                        icon = Icons.Filled.DeleteOutline,
                        label = "Excluir evento",
                        description = "Some do app para todos",
                        isDanger = true,
                        onClick = { confirmDelete = true },
                    ),
                ),
                onDismiss = { showPanel = false },
            )
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Descartar alterações?") },
            text = { Text("O que você mudou neste evento ainda não foi salvo.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onBack() }) { Text("Descartar") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Continuar editando") } },
        )
    }
    if (confirmDelete) {
        DeleteEventDialog(
            onConfirm = { confirmDelete = false; viewModel.onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** The form's two tabs; the choice is screen state only. */
enum class EventFormTab(val label: String) {
    EDIT("Editar"),
    PREVIEW("Como fica no app"),
}

@Composable
fun EventFormContent(
    state: EventFormUiState,
    tab: EventFormTab,
    imageLoader: ImageLoader,
    onSelectTab: (EventFormTab) -> Unit,
    onPickImage: () -> Unit,
    onDraftChanged: (EventDraft) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        state.isLoading -> Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        state.loadError != null -> Box(Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            ) {
                Text(state.loadError, textAlign = TextAlign.Center)
                Button(onClick = onRetry) { Text("Tentar novamente") }
            }
        }
        else -> Column(Modifier.fillMaxSize()) {
            FormTabs(selected = tab, onSelect = onSelectTab)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    EventFormTab.EDIT -> FormFields(state, imageLoader, onPickImage, onDraftChanged)
                    EventFormTab.PREVIEW -> FormPreview(state, imageLoader)
                }
            }
            if (!state.isReadOnly) SaveBar(isSaving = state.isSaving, onSave = onSave)
        }
    }
}

/** "Editar" / "Como fica no app" as a two-way pill under the top bar. */
@Composable
private fun FormTabs(selected: EventFormTab, onSelect: (EventFormTab) -> Unit) {
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        EventFormTab.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(option) })
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun FormFields(
    state: EventFormUiState,
    imageLoader: ImageLoader,
    onPickImage: () -> Unit,
    onChange: (EventDraft) -> Unit,
) {
    val draft = state.draft
    val errors = state.fieldErrors
    val enabled = !state.isReadOnly && !state.isSaving
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        EventImageField(
            pickedImage = state.pickedImage,
            imageUrl = state.imageUrl,
            imageLoader = imageLoader,
            error = errors[EventField.IMAGE],
            enabled = enabled,
            onPick = onPickImage,
        )
        state.generalError?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(12.dp),
            )
        }
        FormGroup("Evento") {
            EventTextField("Título", draft.title, errors[EventField.TITLE], enabled) {
                onChange(draft.copy(title = it))
            }
            EventTextField(
                label = "Descrição",
                value = draft.description,
                error = errors[EventField.DESCRIPTION],
                enabled = enabled,
                singleLine = false,
            ) { onChange(draft.copy(description = it)) }
        }
        FormGroup("Quando e onde") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventDateField(
                    label = "Começa",
                    date = draft.startDate,
                    error = errors[EventField.START_DATE],
                    enabled = enabled,
                    clearable = false,
                    modifier = Modifier.weight(1f),
                ) { onChange(draft.copy(startDate = it)) }
                EventDateField(
                    label = "Termina (opcional)",
                    date = draft.endDate,
                    error = errors[EventField.END_DATE],
                    enabled = enabled,
                    clearable = true,
                    modifier = Modifier.weight(1f),
                ) { onChange(draft.copy(endDate = it)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventTimeField(
                    time = draft.startTime,
                    error = errors[EventField.START_TIME],
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) { onChange(draft.copy(startTime = it)) }
                EventTextField(
                    label = "Local (opcional)",
                    value = draft.location,
                    error = errors[EventField.LOCATION],
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) { onChange(draft.copy(location = it)) }
            }
        }
        FormGroup("Quem vê", card = false) {
            VisibilitySelector(selected = draft.visibility, enabled = enabled) { onChange(draft.copy(visibility = it)) }
            errors[EventField.VISIBILITY]?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        state.historyLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** A titled group: a green bar and the title, then the fields in a card (or bare, for the visibility cards). */
@Composable
private fun FormGroup(title: String, card: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Box(
                Modifier
                    .size(width = 4.dp, height = 15.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = if (card) {
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CardRadius))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(12.dp)
            } else {
                Modifier.fillMaxWidth()
            },
            content = content,
        )
    }
}

/** What the event looks like before saving: the Home highlight card and the top of the event's screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormPreview(state: EventFormUiState, imageLoader: ImageLoader) {
    val draft = state.draft
    val highlightShape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        PreviewLabel("No destaque da Home")
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(PREVIEW_ASPECT)
                .shadow(8.dp, highlightShape)
                .clip(highlightShape),
        ) {
            HighlightEvent(
                page = HighlightEventUi(
                    id = 0,
                    title = draft.title,
                    dateLabel = state.previewDateLabel,
                    imageUrl = state.imageUrl.orEmpty(),
                ),
                imageLoader = imageLoader,
                imageModel = state.pickedImage ?: state.imageUrl,
            )
        }
        PreviewLabel("Ao abrir o evento")
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CardRadius))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(14.dp),
        ) {
            Text(
                text = draft.title.ifBlank { "Sem título" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PreviewTag(state.previewShortDate)
                draft.startTime?.let { PreviewTag(EventDateLabel.timeLabel(it)) }
                draft.location.takeIf { it.isNotBlank() }?.let { PreviewTag(it) }
                if (draft.visibility == EventVisibility.MEMBERS) PreviewTag("Só membros")
            }
            draft.description.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PreviewLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun PreviewTag(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Fixed at the bottom: one tap saves; disabled while saving so two taps never create two events. */
@Composable
private fun SaveBar(isSaving: Boolean, onSave: () -> Unit) {
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
                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Salvar evento")
            }
        }
    }
}

private const val PREVIEW_ASPECT = 3f / 2f
private val CardRadius = 18.dp

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun EventFormContentPreview() {
    EventFormContent(
        state = EventFormUiState(
            isEditing = true,
            draft = EventDraft(title = "Congresso de Jovens", location = "Templo"),
            historyLabel = "Criado por Ana Paula em 21/10/2026 · editado por Gabriel em 22/10/2026",
            fieldErrors = mapOf(EventField.START_DATE to "Informe a data de início."),
        ),
        tab = EventFormTab.EDIT,
        imageLoader = ImageLoader(LocalContext.current),
        onSelectTab = {},
        onPickImage = {},
        onDraftChanged = {},
        onSave = {},
        onRetry = {},
    )
}
