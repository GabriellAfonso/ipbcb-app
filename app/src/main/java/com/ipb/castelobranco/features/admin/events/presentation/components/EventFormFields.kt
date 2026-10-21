package com.ipb.castelobranco.features.admin.events.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import coil.ImageLoader
import coil.compose.AsyncImage
import com.ipb.castelobranco.core.presentation.components.DateFieldWithPicker
import com.ipb.castelobranco.core.presentation.format.EventDateLabel
import com.ipb.castelobranco.features.admin.events.domain.model.EventVisibility
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private const val IMAGE_ASPECT = 16f / 9f
private const val CLEAR_LABEL = "Remover"

@Composable
fun FieldError(message: String?) {
    if (message == null) return
    Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
}

@Composable
fun EventTextField(
    label: String,
    value: String,
    error: String?,
    enabled: Boolean,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            isError = error != null,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        FieldError(error)
    }
}

/** The flyer: the picked one, else the current one, else an invitation to pick. Tapping picks again. */
@Composable
fun EventImageField(
    pickedImage: ByteArray?,
    imageUrl: String?,
    imageLoader: ImageLoader,
    error: String?,
    enabled: Boolean,
    onPick: () -> Unit,
) {
    val picked = remember(pickedImage) {
        pickedImage?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(IMAGE_ASPECT)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(enabled = enabled, onClick = onPick),
        ) {
            when {
                picked != null -> Image(
                    bitmap = picked,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                imageUrl != null -> AsyncImage(
                    model = imageUrl,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Text("Escolher imagem (16:9)", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (enabled && (picked != null || imageUrl != null)) {
            TextButton(onClick = onPick) { Text("Trocar imagem") }
        }
        FieldError(error)
    }
}

/** A date with its own picker; [clearable] adds "Remover" for the optional end date. */
@Composable
fun EventDateField(
    label: String,
    date: LocalDate?,
    error: String?,
    enabled: Boolean,
    clearable: Boolean,
    onChange: (LocalDate?) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            DateFieldWithPicker(
                dateBr = date?.let { EventDateLabel.fullDateLabel(it, null) }.orEmpty(),
                onOpenPicker = { if (enabled) picking = true },
                modifier = Modifier.weight(1f),
            )
            if (clearable && enabled && date != null) {
                TextButton(onClick = { onChange(null) }) { Text(CLEAR_LABEL) }
            }
        }
        FieldError(error)
    }
    if (picking) {
        EventDatePickerDialog(
            initial = date,
            onDismiss = { picking = false },
            onConfirm = { picked -> onChange(picked); picking = false },
        )
    }
}

@Composable
fun EventTimeField(
    time: LocalTime?,
    error: String?,
    enabled: Boolean,
    onChange: (LocalTime?) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Horário", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            DateFieldWithPicker(
                dateBr = time?.let(EventDateLabel::timeLabel).orEmpty(),
                onOpenPicker = { if (enabled) picking = true },
                modifier = Modifier.weight(1f),
                placeholder = "Selecionar horário",
            )
            if (enabled && time != null) TextButton(onClick = { onChange(null) }) { Text(CLEAR_LABEL) }
        }
        FieldError(error)
    }
    if (picking) {
        EventTimePickerDialog(
            initial = time,
            onDismiss = { picking = false },
            onConfirm = { picked -> onChange(picked); picking = false },
        )
    }
}

@Composable
fun VisibilitySelector(
    selected: EventVisibility,
    enabled: Boolean,
    onSelect: (EventVisibility) -> Unit,
) {
    val options = listOf(EventVisibility.PUBLIC to "Público", EventVisibility.MEMBERS to "Só membros")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Visibilidade", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (visibility, label) ->
                SegmentedButton(
                    selected = selected == visibility,
                    onClick = { onSelect(visibility) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) { Text(label) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDatePickerDialog(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTimePickerDialog(
    initial: LocalTime?,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial?.hour ?: DEFAULT_HOUR,
        initialMinute = initial?.minute ?: 0,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = { TimePicker(state = state) },
    )
}

private const val DEFAULT_HOUR = 19
