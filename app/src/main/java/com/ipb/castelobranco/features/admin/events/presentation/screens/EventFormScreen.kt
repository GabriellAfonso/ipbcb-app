package com.ipb.castelobranco.features.admin.events.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.rememberCroppedPhotoPicker
import com.ipb.castelobranco.features.admin.events.domain.model.EventDraft
import com.ipb.castelobranco.features.admin.events.domain.model.EventField
import com.ipb.castelobranco.features.admin.events.presentation.components.DeleteEventDialog
import com.ipb.castelobranco.features.admin.events.presentation.components.EventDateField
import com.ipb.castelobranco.features.admin.events.presentation.components.EventImageField
import com.ipb.castelobranco.features.admin.events.presentation.components.EventTextField
import com.ipb.castelobranco.features.admin.events.presentation.components.EventTimeField
import com.ipb.castelobranco.features.admin.events.presentation.components.VisibilitySelector
import com.ipb.castelobranco.features.admin.events.presentation.state.EventFormUiState
import com.ipb.castelobranco.features.admin.events.presentation.state.EventsAdminEvent
import com.ipb.castelobranco.features.admin.events.presentation.viewmodel.EventFormViewModel

private const val CROP_ASPECT_X = 16f
private const val CROP_ASPECT_Y = 9f
private const val CROP_MAX_WIDTH = 1920
private const val CROP_MAX_HEIGHT = 1080
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
            if (state.canDelete && !state.isLoading) {
                IconButton(onClick = { confirmDelete = true }, enabled = !state.isDeleting) {
                    Icon(Icons.Filled.Delete, contentDescription = "Excluir evento")
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            EventFormContent(
                state = state,
                imageLoader = viewModel.imageLoader,
                onPickImage = pickImage,
                onDraftChanged = viewModel::onDraftChanged,
                onSave = viewModel::onSave,
                onRetry = viewModel::load,
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

@Composable
fun EventFormContent(
    state: EventFormUiState,
    imageLoader: ImageLoader,
    onPickImage: () -> Unit,
    onDraftChanged: (EventDraft) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.loadError != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            ) {
                Text(state.loadError, textAlign = TextAlign.Center)
                Button(onClick = onRetry) { Text("Tentar novamente") }
            }
            else -> FormFields(state, imageLoader, onPickImage, onDraftChanged, onSave)
        }
    }
}

@Composable
private fun FormFields(
    state: EventFormUiState,
    imageLoader: ImageLoader,
    onPickImage: () -> Unit,
    onChange: (EventDraft) -> Unit,
    onSave: () -> Unit,
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
        EventTextField("Título *", draft.title, errors[EventField.TITLE], enabled) {
            onChange(draft.copy(title = it))
        }
        EventDateField(
            label = "Data de início *",
            date = draft.startDate,
            error = errors[EventField.START_DATE],
            enabled = enabled,
            clearable = false,
        ) { onChange(draft.copy(startDate = it)) }
        EventDateField(
            label = "Data de fim (vários dias)",
            date = draft.endDate,
            error = errors[EventField.END_DATE],
            enabled = enabled,
            clearable = true,
        ) { onChange(draft.copy(endDate = it)) }
        EventTimeField(time = draft.startTime, error = errors[EventField.START_TIME], enabled = enabled) {
            onChange(draft.copy(startTime = it))
        }
        EventTextField("Local", draft.location, errors[EventField.LOCATION], enabled) {
            onChange(draft.copy(location = it))
        }
        EventTextField("Descrição", draft.description, errors[EventField.DESCRIPTION], enabled, singleLine = false) {
            onChange(draft.copy(description = it))
        }
        VisibilitySelector(selected = draft.visibility, enabled = enabled) { onChange(draft.copy(visibility = it)) }
        errors[EventField.VISIBILITY]?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (!state.isReadOnly) {
            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Salvar")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventFormContentPreview() {
    EventFormContent(
        state = EventFormUiState(
            draft = EventDraft(title = "Congresso de Jovens", location = "Templo"),
            fieldErrors = mapOf(EventField.START_DATE to "Informe a data de início."),
        ),
        imageLoader = ImageLoader(LocalContext.current),
        onPickImage = {},
        onDraftChanged = {},
        onSave = {},
        onRetry = {},
    )
}
