package com.ipb.castelobranco.features.gallery.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.gallery.domain.model.GalleryMember
import com.ipb.castelobranco.features.gallery.presentation.state.PeoplePickerState
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoImage
import com.ipb.castelobranco.features.gallery.presentation.state.PickerMode
import com.ipb.castelobranco.features.gallery.presentation.state.ViewerPhoto

private const val IN_THIS_PHOTO = "Nesta foto"
private const val NOBODY_TAGGED = "Ninguém marcado"
private const val TAG_PEOPLE_LABEL = "Marcar pessoas"
private const val SEARCH_LABEL = "Buscar pessoa"
private const val CANCEL_LABEL = "Cancelar"
private const val RETRY_LABEL = "Tentar novamente"
private const val LIST_MAX_HEIGHT_DP = 420

/**
 * Who is in the photo, with its description and date. Names are plain text. [canTag] adds
 * "Marcar pessoas" (`manage`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailsSheet(photo: ViewerPhoto, canTag: Boolean, onTag: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        PhotoDetails(photo, canTag, onTag)
    }
}

@Composable
private fun PhotoDetails(photo: ViewerPhoto, canTag: Boolean, onTag: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(photo.title, style = MaterialTheme.typography.titleMedium)
        photo.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        photo.dateTaken?.let { Text("Tirada em $it", style = MaterialTheme.typography.bodySmall) }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        Text(IN_THIS_PHOTO, style = MaterialTheme.typography.labelLarge)
        if (photo.people.isEmpty()) {
            Text(NOBODY_TAGGED, style = MaterialTheme.typography.bodyMedium)
        } else {
            photo.people.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        if (canTag) {
            OutlinedButton(onClick = onTag, modifier = Modifier.padding(top = 8.dp)) { Text(TAG_PEOPLE_LABEL) }
        }
    }
}

/** The events of the people picker. */
data class PeoplePickerActions(
    val onQueryChange: (String) -> Unit,
    val onToggle: (Long) -> Unit,
    val onRetry: () -> Unit,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
)

/** Pick people: search, multi-select, confirm. Loading, error with retry, empty and list states. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeoplePickerSheet(state: PeoplePickerState, actions: PeoplePickerActions) {
    ModalBottomSheet(onDismissRequest = actions.onDismiss) {
        PeoplePicker(state, actions)
    }
}

@Composable
private fun PeoplePicker(state: PeoplePickerState, actions: PeoplePickerActions) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
        Text(state.title, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.query,
            onValueChange = actions.onQueryChange,
            label = { Text(SEARCH_LABEL) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        val emptyText = state.emptyText
        Box(modifier = Modifier.fillMaxWidth().heightIn(max = LIST_MAX_HEIGHT_DP.dp)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center).padding(24.dp))
                state.error != null -> Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.error, textAlign = TextAlign.Center)
                    OutlinedButton(onClick = actions.onRetry, modifier = Modifier.padding(top = 12.dp)) {
                        Text(RETRY_LABEL)
                    }
                }
                emptyText != null -> Text(
                    text = emptyText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
                else -> LazyColumn {
                    items(state.visiblePeople, key = { it.id }) { person ->
                        PersonCheckRow(
                            name = person.name,
                            detail = null,
                            checked = person.id in state.checked,
                            enabled = !state.isSaving,
                            onClick = { actions.onToggle(person.id) },
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = actions.onDismiss, enabled = !state.isSaving) { Text(CANCEL_LABEL) }
            Button(onClick = actions.onConfirm, enabled = state.canConfirm, modifier = Modifier.padding(start = 8.dp)) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(state.confirmLabel)
                }
            }
        }
    }
}

/** A person with a checkbox; [detail] e.g. "3 fotos". */
@Composable
fun PersonCheckRow(name: String, detail: String?, checked: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled, modifier = Modifier.padding(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

// region previews

private val previewPhoto = ViewerPhoto(
    id = 1,
    title = "IMG_0042",
    fileName = "IMG_0042.jpg",
    image = PhotoImage.None,
    albumId = 7,
    description = "Culto de encerramento",
    dateTaken = "14/03/2026",
    people = listOf("João Lima", "Maria Souza"),
)

private val previewPickerActions = PeoplePickerActions({}, {}, {}, {}, {})

@Preview(showBackground = true)
@Composable
private fun PhotoDetailsPreview() {
    PhotoDetails(previewPhoto, canTag = true, onTag = {})
}

@Preview(showBackground = true)
@Composable
private fun PhotoDetailsUntaggedPreview() {
    PhotoDetails(previewPhoto.copy(description = null, people = emptyList()), canTag = false, onTag = {})
}

@Preview(showBackground = true)
@Composable
private fun PeoplePickerPreview() {
    PeoplePicker(
        PeoplePickerState(
            mode = PickerMode.Photo(1),
            people = listOf(GalleryMember(12, "Maria Souza"), GalleryMember(40, "João Lima")),
            checked = setOf(12),
        ),
        previewPickerActions,
    )
}

@Preview(showBackground = true)
@Composable
private fun PeoplePickerErrorPreview() {
    PeoplePicker(
        PeoplePickerState(mode = PickerMode.Add(7, listOf(1, 2)), error = "Sem conexão"),
        previewPickerActions,
    )
}

// endregion
