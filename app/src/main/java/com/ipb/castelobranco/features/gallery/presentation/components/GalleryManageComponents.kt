package com.ipb.castelobranco.features.gallery.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.gallery.domain.model.TreeTarget
import com.ipb.castelobranco.features.gallery.presentation.state.AlbumUploadsUiState
import com.ipb.castelobranco.features.gallery.presentation.state.ConfirmAction
import com.ipb.castelobranco.features.gallery.presentation.state.ConfirmState
import com.ipb.castelobranco.features.gallery.presentation.state.FailedUpload
import com.ipb.castelobranco.features.gallery.presentation.state.FormTarget
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryDialogState
import com.ipb.castelobranco.features.gallery.presentation.state.ItemFormState
import com.ipb.castelobranco.features.gallery.presentation.state.MovePickerState
import com.ipb.castelobranco.features.gallery.presentation.state.MoveSubject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val SAVE_LABEL = "Salvar"
private const val CANCEL_LABEL = "Cancelar"
private const val NAME_LABEL = "Nome"
private const val DESCRIPTION_LABEL = "Descrição (opcional)"
private const val NO_DATE_LABEL = "Sem data"
private const val CLEAR_DATE_LABEL = "Limpar data"
private const val PICK_DATE_LABEL = "Escolher"
private const val OK_LABEL = "OK"
private const val MORE_LABEL = "Mais opções"
private const val MOVE_LABEL = "Mover"
private const val DELETE_LABEL = "Apagar"
private const val CLOSE_LABEL = "Fechar"
private const val PEOPLE_LABEL = "Pessoas"
private const val ADD_PEOPLE_LABEL = "Adicionar pessoas"
private const val REMOVE_PEOPLE_LABEL = "Remover pessoas"
private const val PREPARING_TEXT = "Preparando fotos…"
private const val DISMISS_LABEL = "Dispensar"
private const val FAILED_TITLE = "Não enviadas"
private const val INDENT_PER_LEVEL_DP = 16
private val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** One menu entry of a management menu. */
data class ManageAction(val label: String, val onClick: () -> Unit)

/** Renders whichever dialog the gallery has open. Pure: state in, events out. */
@Composable
fun GalleryDialogHost(dialog: GalleryDialogState?, actions: GalleryDialogActions) {
    when (dialog) {
        is GalleryDialogState.Form -> ItemFormDialog(dialog.form, actions)
        is GalleryDialogState.MovePicker -> MoveTargetSheet(dialog.picker, actions.onChooseTarget, actions.onDismiss)
        is GalleryDialogState.Confirm -> ConfirmDialog(dialog.confirm, actions.onConfirm, actions.onDismiss)
        is GalleryDialogState.PeoplePicker -> PeoplePickerSheet(dialog.picker, actions.picker)
        null -> Unit
    }
}

/** The events of every gallery dialog. */
data class GalleryDialogActions(
    val onNameChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onDateChange: (String?) -> Unit,
    val onSave: () -> Unit,
    val onChooseTarget: (TreeTarget) -> Unit,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
    val picker: PeoplePickerActions = PeoplePickerActions({}, {}, {}, {}, onDismiss),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormDialog(form: ItemFormState, actions: GalleryDialogActions) {
    var pickingDate by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(form.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = actions.onNameChange,
                    label = { Text(NAME_LABEL) },
                    singleLine = true,
                    isError = form.nameError != null,
                    supportingText = form.nameError?.let { error -> { Text(error) } },
                    enabled = !form.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.description,
                    onValueChange = actions.onDescriptionChange,
                    label = { Text(DESCRIPTION_LABEL) },
                    minLines = 2,
                    enabled = !form.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(form.dateLabel, style = MaterialTheme.typography.labelMedium)
                        Text(form.date?.let(::displayDate) ?: NO_DATE_LABEL, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (form.date != null) {
                        TextButton(onClick = { actions.onDateChange(null) }, enabled = !form.isSaving) {
                            Text(CLEAR_DATE_LABEL)
                        }
                    }
                    TextButton(onClick = { pickingDate = true }, enabled = !form.isSaving) { Text(PICK_DATE_LABEL) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onSave, enabled = !form.isSaving && form.name.isNotBlank()) {
                if (form.isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(SAVE_LABEL)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onDismiss, enabled = !form.isSaving) { Text(CANCEL_LABEL) }
        },
    )

    if (pickingDate) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = form.date?.let(::toMillis))
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { actions.onDateChange(fromMillis(it)) }
                    pickingDate = false
                }) { Text(OK_LABEL) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(CANCEL_LABEL) } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun ConfirmDialog(state: ConfirmState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(state.text) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !state.isRunning) {
                if (state.isRunning) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(state.confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.isRunning) { Text(CANCEL_LABEL) } },
    )
}

/** The album tree to pick a destination from; rows indented by depth, unavailable ones greyed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveTargetSheet(state: MovePickerState, onChoose: (TreeTarget) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        if (state.isSaving) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
            items(state.targets, key = { it.albumId ?: ROOT_KEY }) { target ->
                MoveTargetRow(target, enabled = target.selectable && !state.isSaving, onClick = { onChoose(target) })
            }
        }
    }
}

private const val ROOT_KEY = -1L

@Composable
private fun MoveTargetRow(target: TreeTarget, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = target.name,
        style = MaterialTheme.typography.bodyLarge,
        color = if (target.selectable) MaterialTheme.colorScheme.onSurface else Color.Gray,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = (24 + target.depth * INDENT_PER_LEVEL_DP).dp, end = 24.dp, top = 14.dp, bottom = 14.dp),
    )
}

/** A top-bar overflow menu; nothing is composed when [actions] is empty. */
@Composable
fun ManageOverflowMenu(actions: List<ManageAction>) {
    if (actions.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = MORE_LABEL) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}

/** "Pessoas" on the selection bar: add people to, or remove people from, the selected photos. */
data class SelectionPeopleActions(
    /** Someone is tagged in a selected photo. */
    val canRemove: Boolean,
    val onAdd: () -> Unit,
    val onRemove: () -> Unit,
)

/** Top-bar actions while photos are selected: people, move, delete (owner) and close. */
@Composable
fun SelectionActions(
    canDelete: Boolean,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    people: SelectionPeopleActions? = null,
) {
    people?.let { SelectionPeopleMenu(it) }
    IconButton(onClick = onMove) { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = MOVE_LABEL) }
    if (canDelete) {
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = DELETE_LABEL) }
    }
    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = CLOSE_LABEL) }
}

@Composable
private fun SelectionPeopleMenu(people: SelectionPeopleActions) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Filled.People, contentDescription = PEOPLE_LABEL) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(ADD_PEOPLE_LABEL) }, onClick = {
                expanded = false
                people.onAdd()
            })
            DropdownMenuItem(text = { Text(REMOVE_PEOPLE_LABEL) }, enabled = people.canRemove, onClick = {
                expanded = false
                people.onRemove()
            })
        }
    }
}

/** Top-bar actions in "Organizar": cancel and save. */
@Composable
fun OrganizeActions(isSaving: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    TextButton(onClick = onCancel, enabled = !isSaving) { Text(CANCEL_LABEL, color = Color.White) }
    TextButton(onClick = onSave, enabled = !isSaving) {
        if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
        else Text(SAVE_LABEL, color = Color.White)
    }
}

/** The album's upload progress and the photos that could not be sent. */
@Composable
fun AlbumUploadsPanel(uploads: AlbumUploadsUiState, onDismiss: (String) -> Unit) {
    if (!uploads.isVisible) return
    Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            if (uploads.isCopying || uploads.pending > 0) {
                Text(
                    text = progressText(uploads),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                if (uploads.total > 0) {
                    LinearProgressIndicator(
                        progress = { (uploads.current - 1).coerceAtLeast(0).toFloat() / uploads.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            if (uploads.failed.isNotEmpty()) {
                Text(
                    text = FAILED_TITLE,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
                uploads.failed.forEach { failed -> FailedUploadRow(failed, onDismiss) }
            }
        }
    }
}

@Composable
private fun FailedUploadRow(failed: FailedUpload, onDismiss: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = failed.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(failed.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        IconButton(onClick = { onDismiss(failed.uploadId) }) {
            Icon(Icons.Filled.Close, contentDescription = DISMISS_LABEL)
        }
    }
}

private fun progressText(uploads: AlbumUploadsUiState): String = when {
    uploads.total > 0 -> "Enviando ${uploads.current} de ${uploads.total}"
    uploads.pending > 0 -> if (uploads.pending == 1) "1 foto na fila" else "${uploads.pending} fotos na fila"
    else -> PREPARING_TEXT
}

private fun displayDate(raw: String): String =
    runCatching { LocalDate.parse(raw).format(DISPLAY_DATE) }.getOrDefault(raw)

private fun toMillis(raw: String): Long? =
    runCatching { LocalDate.parse(raw).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()

private fun fromMillis(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()

// region previews

private val previewActions = GalleryDialogActions({}, {}, {}, {}, {}, {}, {})

@Preview
@Composable
private fun ItemFormDialogPreview() {
    ItemFormDialog(
        ItemFormState(FormTarget.Album(1), name = "Retiro 2026", description = "Sítio", date = "2026-03-14"),
        previewActions,
    )
}

@Preview
@Composable
private fun ItemFormDialogErrorPreview() {
    ItemFormDialog(
        ItemFormState(FormTarget.NewAlbum(null), name = "Sábado", nameError = "Já existe um álbum com esse nome."),
        previewActions,
    )
}

@Preview
@Composable
private fun ConfirmDialogPreview() {
    ConfirmDialog(
        ConfirmState(
            ConfirmAction.DeleteAlbum(1),
            "Apagar 'Retiro' com 2 subálbuns e 41 fotos? Fica 30 dias na lixeira.",
            DELETE_LABEL,
        ),
        onConfirm = {},
        onDismiss = {},
    )
}

@Preview
@Composable
private fun MoveTargetRowsPreview() {
    val state = MovePickerState(
        MoveSubject.Album(2),
        listOf(
            TreeTarget(null, "Raiz", 0, true),
            TreeTarget(1, "Retiro 2026", 1, false),
            TreeTarget(4, "Domingo", 2, true),
        ),
    )
    Column { state.targets.forEach { MoveTargetRow(it, enabled = it.selectable, onClick = {}) } }
}

@Preview
@Composable
private fun AlbumUploadsPanelPreview() {
    AlbumUploadsPanel(
        AlbumUploadsUiState(
            pending = 17,
            current = 3,
            total = 20,
            failed = listOf(FailedUpload("u1", "IMG_0042.jpg", "A imagem excede 10 MB.")),
        ),
        onDismiss = {},
    )
}

// endregion
