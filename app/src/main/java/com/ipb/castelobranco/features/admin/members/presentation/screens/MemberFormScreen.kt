package com.ipb.castelobranco.features.admin.members.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.rememberSquarePhotoPicker
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.features.admin.members.domain.model.MemberDraft
import com.ipb.castelobranco.features.admin.members.domain.model.MemberField
import com.ipb.castelobranco.features.admin.members.domain.model.MemberOptions
import com.ipb.castelobranco.features.admin.members.domain.model.NamedRef
import com.ipb.castelobranco.features.admin.members.presentation.components.BirthDateField
import com.ipb.castelobranco.features.admin.members.presentation.components.FieldError
import com.ipb.castelobranco.features.admin.members.presentation.components.GenderSelector
import com.ipb.castelobranco.features.admin.members.presentation.components.InvalidContainer
import com.ipb.castelobranco.features.admin.members.presentation.components.MemberAvatar
import com.ipb.castelobranco.features.admin.members.presentation.components.MemberDateField
import com.ipb.castelobranco.features.admin.members.presentation.components.MinistriesPicker
import com.ipb.castelobranco.features.admin.members.presentation.components.OnInvalidContainer
import com.ipb.castelobranco.features.admin.members.presentation.components.OptionPicker
import com.ipb.castelobranco.features.admin.members.presentation.state.MemberFormUiState
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.presentation.viewmodel.MemberFormViewModel

@Composable
fun MemberFormScreen(
    viewModel: MemberFormViewModel,
    onBack: () -> Unit,
    onEvent: (MembersEvent) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDiscard by remember { mutableStateOf(false) }
    val pickPhoto = rememberSquarePhotoPicker(onPicked = viewModel::onPhotoPicked)
    val leave = { if (state.hasUnsavedChanges) confirmDiscard = true else onBack() }

    BackHandler(enabled = state.hasUnsavedChanges) { confirmDiscard = true }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MembersEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> onEvent(event)
            }
        }
    }

    BaseScreen(
        tabName = if (state.isEditing) "Editar membro" else "Novo membro",
        showBackArrow = true,
        onBackClick = leave,
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            MemberFormContent(
                state = state,
                imageLoader = viewModel.imageLoader,
                onPickPhoto = pickPhoto,
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
            text = { Text("O que você mudou neste membro ainda não foi salvo.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onBack() }) { Text("Descartar") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Continuar editando") } },
        )
    }
}

@Composable
fun MemberFormContent(
    state: MemberFormUiState,
    imageLoader: ImageLoader?,
    onPickPhoto: () -> Unit,
    onDraftChanged: (MemberDraft) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    val options = state.options
    Box(Modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            options == null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            ) {
                Text(state.loadError.orEmpty(), textAlign = TextAlign.Center)
                Button(onClick = onRetry) { Text("Tentar novamente") }
            }
            else -> FormFields(state, options, imageLoader, onPickPhoto, onDraftChanged, onSave)
        }
    }
}

@Composable
private fun FormFields(
    state: MemberFormUiState,
    options: MemberOptions,
    imageLoader: ImageLoader?,
    onPickPhoto: () -> Unit,
    onChange: (MemberDraft) -> Unit,
    onSave: () -> Unit,
) {
    val draft = state.draft
    val errors = state.fieldErrors
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        FormPhoto(state, imageLoader, onPickPhoto, Modifier.align(Alignment.CenterHorizontally))
        state.generalError?.let { message ->
            Text(
                text = message,
                color = OnInvalidContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(InvalidContainer)
                    .padding(12.dp),
            )
        }
        TextInput("Nome de exibição *", draft.name, errors[MemberField.NAME]) { onChange(draft.copy(name = it)) }
        TextInput("Primeiro nome", draft.firstName, errors[MemberField.FIRST_NAME]) {
            onChange(draft.copy(firstName = it))
        }
        TextInput("Sobrenome", draft.lastName, errors[MemberField.LAST_NAME]) { onChange(draft.copy(lastName = it)) }
        BirthDateField(
            birth = draft.birth,
            onChange = { onChange(draft.copy(birth = it)) },
            birthdayError = errors[MemberField.BIRTH_DAY] ?: errors[MemberField.BIRTH_MONTH],
            yearError = errors[MemberField.BIRTH_YEAR],
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Sexo", style = MaterialTheme.typography.labelLarge)
            GenderSelector(selected = draft.gender, onSelect = { onChange(draft.copy(gender = it)) })
            FieldError(errors[MemberField.GENDER])
        }
        OptionPicker(
            label = "Situação",
            options = options.statuses,
            selectedId = draft.statusId,
            onSelect = { onChange(draft.copy(statusId = it)) },
            error = errors[MemberField.STATUS],
        )
        OptionPicker(
            label = "Cargo",
            options = options.roles,
            selectedId = draft.roleId,
            onSelect = { onChange(draft.copy(roleId = it)) },
            error = errors[MemberField.ROLE],
        )
        MinistriesPicker(
            options = options.ministries,
            selectedIds = draft.ministryIds,
            onChange = { onChange(draft.copy(ministryIds = it)) },
            error = errors[MemberField.MINISTRIES],
        )
        MemberDateField(
            label = "Batismo",
            date = draft.baptismDate,
            onChange = { onChange(draft.copy(baptismDate = it)) },
            error = errors[MemberField.BAPTISM_DATE],
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Perfil válido", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Perfil inválido não aparece na lista de membros nem nos aniversários.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = draft.isValid, onCheckedChange = { onChange(draft.copy(isValid = it)) })
        }
        Button(
            onClick = onSave,
            enabled = !state.isSaving,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Salvar")
            }
        }
    }
}

/** Previews a picked photo; it is uploaded only on "Salvar". */
@Composable
private fun FormPhoto(
    state: MemberFormUiState,
    imageLoader: ImageLoader?,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier) {
        MemberAvatar(
            initials = state.initials,
            photoUrl = state.photoUrl,
            imageLoader = imageLoader,
            pickedPhoto = state.pickedPhoto,
            initialsSize = 32.sp,
            modifier = Modifier
                .size(FORM_PHOTO_SIZE.dp)
                .clip(CircleShape)
                .clickable(enabled = !state.isSaving, onClick = onPickPhoto),
        )
        Surface(
            onClick = onPickPhoto,
            enabled = !state.isSaving,
            shape = CircleShape,
            color = colors.primary,
            contentColor = colors.onPrimary,
            border = BorderStroke(3.dp, colors.background),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = "Trocar foto", Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TextInput(label: String, value: String, error: String?, onValueChange: (String) -> Unit) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            isError = error != null,
            modifier = Modifier.fillMaxWidth(),
        )
        FieldError(error)
    }
}

private const val FORM_PHOTO_SIZE = 96

@Preview(showBackground = true, widthDp = 390, heightDp = 1400)
@Composable
private fun MemberFormContentPreview() {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        MemberFormContent(
            state = MemberFormUiState(
                isEditing = true,
                initials = "AS",
                isLoading = false,
                draft = MemberDraft(name = "", statusId = 3),
                options = MemberOptions(
                    statuses = listOf(NamedRef(1, "Ativo"), NamedRef(2, "Inativo"), NamedRef(3, "Visitante")),
                    roles = listOf(NamedRef(4, "Diaconisa")),
                    ministries = listOf(NamedRef(2, "Louvor"), NamedRef(5, "Recepção")),
                ),
                fieldErrors = mapOf(MemberField.NAME to "Informe o nome."),
            ),
            imageLoader = null,
            onPickPhoto = {},
            onDraftChanged = {},
            onSave = {},
            onRetry = {},
        )
    }
}
