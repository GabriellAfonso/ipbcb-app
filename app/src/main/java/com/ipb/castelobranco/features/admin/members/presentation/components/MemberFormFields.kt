package com.ipb.castelobranco.features.admin.members.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.core.presentation.components.DateFieldWithPicker
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
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

@Composable
fun FieldError(message: String?) {
    if (message == null) return
    Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
}

/** Masculino / Feminino / Não informado. */
@Composable
fun GenderSelector(selected: Gender?, onSelect: (Gender?) -> Unit) {
    val choices = listOf<Pair<Gender?, String>>(
        Gender.MALE to Gender.MALE.label,
        Gender.FEMALE to Gender.FEMALE.label,
        null to NOT_INFORMED,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { (gender, label) ->
            FilterChip(
                selected = selected == gender,
                onClick = { onSelect(gender) },
                label = { Text(label) },
                leadingIcon = if (selected == gender) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else {
                    null
                },
            )
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
    error: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.id == selectedId }?.name ?: NONE
    Column {
        Box {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true },
            )
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

/** Several ministries at once; the chosen set replaces the member's whole list. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MinistriesPicker(
    options: List<NamedRef>,
    selectedIds: Set<Int>,
    onChange: (Set<Int>) -> Unit,
    error: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Ministérios", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val selected = option.id in selectedIds
                FilterChip(
                    selected = selected,
                    onClick = { onChange(if (selected) selectedIds - option.id else selectedIds + option.id) },
                    label = { Text(option.name) },
                )
            }
        }
        FieldError(error)
    }
}

/** A full date through the calendar, with a way to clear it. */
@Composable
fun MemberDateField(
    label: String,
    date: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    error: String? = null,
) {
    var picking by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            DateFieldWithPicker(
                dateBr = date?.let(::formatFullDate).orEmpty(),
                onOpenPicker = { picking = true },
                modifier = Modifier.weight(1f),
            )
            if (date != null) TextButton(onClick = { onChange(null) }) { Text("Limpar") }
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
 * year. Clearing one keeps the other. The day list follows the month and the year, so 29 February
 * is offered with no year or a leap year.
 */
@Composable
fun BirthDateField(
    birth: BirthDate,
    onChange: (BirthDate) -> Unit,
    birthdayError: String? = null,
    yearError: String? = null,
) {
    val (day, month, year) = birth
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Nascimento", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                NumberDropdown(
                    label = "Dia",
                    value = day?.toString(),
                    values = (1..maxDay(month ?: 1, year)).map { it to it.toString() },
                    onSelect = { picked -> onChange(birth.copy(day = picked, month = month ?: 1)) },
                )
            }
            Box(Modifier.weight(2f)) {
                NumberDropdown(
                    label = "Mês",
                    value = month?.let(::monthName),
                    values = (1..12).map { it to monthName(it) },
                    onSelect = { picked ->
                        onChange(birth.copy(day = minOf(day ?: 1, maxDay(picked, year)), month = picked))
                    },
                )
            }
            if (birth.hasBirthday) {
                TextButton(onClick = { onChange(birth.copy(day = null, month = null)) }) { Text("Limpar") }
            }
        }
        FieldError(birthdayError)
        OutlinedTextField(
            value = year?.toString().orEmpty(),
            onValueChange = { text ->
                val digits = text.filter(Char::isDigit).take(YEAR_DIGITS)
                onChange(birth.copy(year = digits.toIntOrNull()))
            },
            label = { Text("Ano") },
            placeholder = { Text("Não informado") },
            singleLine = true,
            isError = yearError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        FieldError(yearError)
    }
}

@Composable
private fun NumberDropdown(
    label: String,
    value: String?,
    values: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true },
        )
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
