package com.ipb.castelobranco.features.admin.members.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.members.domain.model.BirthDate
import com.ipb.castelobranco.features.admin.members.domain.model.Gender
import com.ipb.castelobranco.features.admin.members.domain.model.NamedRef
import com.ipb.castelobranco.features.admin.members.presentation.util.NOT_INFORMED
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

private const val NONE = "Nenhum"
private const val YEAR_DIGITS = 4

/** A year shorter than this is still being typed; it does not narrow February yet. */
private const val MIN_FULL_YEAR = 1000
private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private val FieldShape = RoundedCornerShape(14.dp)

@Composable
fun FieldError(message: String?) {
    if (message == null) return
    Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
}

/** A tonal text field: filled, no underline, rounded. */
@Composable
fun TonalTextField(
    label: String,
    value: String,
    error: String?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onValueChange: (String) -> Unit,
) {
    Column(modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            singleLine = true,
            isError = error != null,
            keyboardOptions = keyboardOptions,
            shape = FieldShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        FieldError(error)
    }
}

/** The tonal box behind a picker: label on top, the value (or [placeholder]) and a [trailing] action. */
@Composable
private fun TonalBox(
    label: String,
    value: String?,
    placeholder: String,
    error: String?,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
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
            .clickable(onClick = onClick)
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
        trailing()
    }
}

/** Masculino / Feminino / Não informado as a three-way pill. */
@Composable
fun GenderSelector(selected: Gender?, onSelect: (Gender?) -> Unit) {
    val choices = listOf<Pair<Gender?, String>>(
        Gender.MALE to Gender.MALE.label,
        Gender.FEMALE to Gender.FEMALE.label,
        null to NOT_INFORMED,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        choices.forEach { (gender, label) ->
            val isSelected = selected == gender
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(gender) })
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

/** One choice from the server's list, or none. */
@Composable
fun OptionPicker(
    label: String,
    options: List<NamedRef>,
    selectedId: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.id == selectedId }?.name ?: NONE
    Column(modifier) {
        Box {
            TonalBox(
                label = label,
                value = selectedName,
                placeholder = NONE,
                error = error,
                onClick = { expanded = true },
            ) {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text(NONE) }, onClick = { onSelect(null); expanded = false })
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = { onSelect(option.id); expanded = false },
                    )
                }
            }
        }
        FieldError(error)
    }
}

/** Several ministries at once, as tags that turn green; the chosen set replaces the member's whole list. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MinistriesPicker(
    options: List<NamedRef>,
    selectedIds: Set<Int>,
    onChange: (Set<Int>) -> Unit,
    error: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Ministérios",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                val selected = option.id in selectedIds
                val shape = RoundedCornerShape(50)
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(shape)
                        .then(
                            if (selected) {
                                Modifier.background(ipbGreen)
                            } else {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                            },
                        )
                        .selectable(
                            selected = selected,
                            role = Role.Checkbox,
                            onClick = { onChange(if (selected) selectedIds - option.id else selectedIds + option.id) },
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        FieldError(error)
    }
}

/** A full date through the calendar, with an "×" to clear it. */
@Composable
fun MemberDateField(
    label: String,
    date: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    error: String? = null,
) {
    var picking by remember { mutableStateOf(false) }
    Column {
        TonalBox(
            label = label,
            value = date?.let(::formatFullDate),
            placeholder = "Escolher",
            error = error,
            onClick = { picking = true },
        ) {
            if (date != null) {
                IconButton(onClick = { onChange(null) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Limpar", modifier = Modifier.size(18.dp))
                }
            }
        }
        FieldError(error)
    }
    if (picking) {
        MemberDatePickerDialog(
            initial = date,
            onDismiss = { picking = false },
            onConfirm = { picked -> onChange(picked); picking = false },
        )
    }
}

/**
 * Birth date as two optional inputs: the birthday (day and month, always set together) and the
 * year, side by side. Clearing one keeps the other. The day list follows the month and the year, so
 * 29 February is offered with no year or a leap year.
 */
@Composable
fun BirthDateField(
    birth: BirthDate,
    onChange: (BirthDate) -> Unit,
    birthdayError: String? = null,
    yearError: String? = null,
) {
    val (day, month, year) = birth
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(0.9f)) {
                NumberDropdown(
                    label = "Dia",
                    value = day?.let { "%02d".format(it) },
                    values = (1..maxDay(month ?: 1, year)).map { it to it.toString() },
                    error = birthdayError,
                    onSelect = { picked -> onChange(birth.copy(day = picked, month = month ?: 1)) },
                )
            }
            Box(Modifier.weight(1.3f)) {
                NumberDropdown(
                    label = "Mês",
                    value = month?.let(::monthName),
                    values = (1..12).map { it to monthName(it) },
                    error = birthdayError,
                    onSelect = { picked ->
                        onChange(birth.copy(day = minOf(day ?: 1, maxDay(picked, year)), month = picked))
                    },
                )
            }
            TonalTextField(
                label = "Ano",
                value = year?.toString().orEmpty(),
                error = null,
                placeholder = "—",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            ) { text ->
                val digits = text.filter(Char::isDigit).take(YEAR_DIGITS)
                onChange(birth.copy(year = digits.toIntOrNull()))
            }
        }
        FieldError(birthdayError)
        FieldError(yearError)
        if (birth.hasBirthday) {
            TextButton(onClick = { onChange(birth.copy(day = null, month = null)) }) { Text("Limpar dia e mês") }
        }
    }
}

@Composable
private fun NumberDropdown(
    label: String,
    value: String?,
    values: List<Pair<Int, String>>,
    error: String?,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TonalBox(label = label, value = value, placeholder = "—", error = error, onClick = { expanded = true }) {
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { (number, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(number); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberDatePickerDialog(
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

/** With no year (or one still being typed) February offers 29: it exists in some leap year. */
private fun maxDay(month: Int, year: Int?): Int =
    if (year != null && year >= MIN_FULL_YEAR) {
        YearMonth.of(year, month).lengthOfMonth()
    } else {
        Month.of(month).maxLength()
    }

private fun monthName(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.FULL, PT_BR).replaceFirstChar { it.titlecase(PT_BR) }

private fun formatFullDate(date: LocalDate): String =
    "%02d/%02d/%04d".format(PT_BR, date.dayOfMonth, date.monthValue, date.year)

@Preview(showBackground = true)
@Composable
private fun MemberFormFieldsPreview() {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GenderSelector(selected = Gender.FEMALE, onSelect = {})
            BirthDateField(birth = BirthDate(day = 2, month = 4), onChange = {})
            MinistriesPicker(
                options = listOf(NamedRef(2, "Louvor"), NamedRef(5, "Recepção")),
                selectedIds = setOf(2),
                onChange = {},
            )
        }
    }
}
