package com.ipb.castelobranco.features.admin.events.presentation.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.ipb.castelobranco.core.presentation.format.EventDateLabel
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.events.domain.model.EventVisibility
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private const val IMAGE_ASPECT = 3f / 2f
private const val CLEAR_LABEL = "Remover"
private const val OVERLAY_ALPHA = 0.55f
private val FieldShape = RoundedCornerShape(14.dp)

@Composable
fun FieldError(message: String?) {
    if (message == null) return
    Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
}

/** A tonal text field: filled, no underline, rounded. */
@Composable
fun EventTextField(
    label: String,
    value: String,
    error: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            isError = error != null,
            enabled = enabled,
            shape = FieldShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        FieldError(error)
    }
}

/**
 * The flyer, large: the picked one, else the current one, else an invitation to pick. "Trocar imagem" sits over it;
 * tapping anywhere picks again.
 */
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
                .clip(RoundedCornerShape(18.dp))
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
                    Text("Escolher imagem (3:2)", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (enabled && (picked != null || imageUrl != null)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = OVERLAY_ALPHA))
                        .clickable(onClick = onPick)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Trocar imagem", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        FieldError(error)
    }
}

/** A tonal field that opens a picker: label on top, value or [placeholder], and an "×" when [onClear] is set. */
@Composable
private fun PickerField(
    label: String,
    value: String?,
    placeholder: String,
    error: String?,
    enabled: Boolean,
    onOpen: () -> Unit,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(FieldShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(
                    if (error != null) Modifier.border(2.dp, MaterialTheme.colorScheme.error, FieldShape) else Modifier,
                )
                .clickable(enabled = enabled, onClick = onOpen)
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onClear != null && enabled && value != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = CLEAR_LABEL, modifier = Modifier.size(18.dp))
                }
            }
        }
        FieldError(error)
    }
}

/** A date with its own picker; [clearable] adds the "×" for the optional end date. */
@Composable
fun EventDateField(
    label: String,
    date: LocalDate?,
    error: String?,
    enabled: Boolean,
    clearable: Boolean,
    modifier: Modifier = Modifier,
    onChange: (LocalDate?) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    PickerField(
        label = label,
        value = date?.let { "%02d/%02d/%d".format(it.dayOfMonth, it.monthValue, it.year) },
        placeholder = "Escolher",
        error = error,
        enabled = enabled,
        onOpen = { picking = true },
        onClear = if (clearable) ({ onChange(null) }) else null,
        modifier = modifier,
    )
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
    modifier: Modifier = Modifier,
    onChange: (LocalTime?) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    PickerField(
        label = "Horário (opcional)",
        value = time?.let(EventDateLabel::timeLabel),
        placeholder = "Escolher",
        error = error,
        enabled = enabled,
        onOpen = { picking = true },
        onClear = { onChange(null) },
        modifier = modifier,
    )
    if (picking) {
        EventTimePickerDialog(
            initial = time,
            onDismiss = { picking = false },
            onConfirm = { picked -> onChange(picked); picking = false },
        )
    }
}

/** "Quem vê": two explained cards; the chosen one gets a green border. */
@Composable
fun VisibilitySelector(
    selected: EventVisibility,
    enabled: Boolean,
    onSelect: (EventVisibility) -> Unit,
) {
    val options = listOf(
        Triple(EventVisibility.PUBLIC, "Todos", "Aparece para qualquer pessoa no app"),
        Triple(EventVisibility.MEMBERS, "Só membros", "Quem não é membro não vê"),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (visibility, title, description) ->
            val isSelected = selected == visibility
            val shape = RoundedCornerShape(14.dp)
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    )
                    .border(2.dp, if (isSelected) ipbGreen else Color.Transparent, shape)
                    .selectable(
                        selected = isSelected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(visibility) },
                    )
                    .padding(10.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
