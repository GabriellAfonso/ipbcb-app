package com.ipb.castelobranco.features.worshiphub.shared.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.domain.constants.MUSIC_TONES
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.presentation.components.ResizeForKeyboard
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.theme.ipbGreen

// The worship hub's forms (new song, chord chart and lyrics), "com prévia" (`specs/worshiphub/spec.md` §3.3).

private const val SONG_LABEL = "Música"
private const val SONG_PLACEHOLDER = "Digite o nome"
private const val NO_SONG_FOUND = "Nenhuma música com esse nome. Cadastre a música antes."
private const val CHANGE_SONG = "Trocar"
private const val TONE_LABEL = "Tom"
private const val INSTRUMENT_LABEL = "Instrumento"
private const val OTHER_INSTRUMENT_LABEL = "Outro instrumento"
private const val OTHER_INSTRUMENT_PLACEHOLDER = "Ex.: Ukulele"
private const val WRITE_TAB = "Escrever"
private const val PREVIEW_TAB = "Prévia"
private const val MISSING_PREFIX = "Falta: "
private const val SAVING_LABEL = "Salvando…"
private const val MAX_SONG_RESULTS = 8
private const val TONE_COLUMNS = 6
private const val DIVIDER_ALPHA = 0.08f

/** Baixo first: the most used. */
private val INSTRUMENTS = listOf("Baixo", "Violão")

private val FieldCorner = 16.dp
private val CardCorner = 16.dp
private val EditorMinHeight = 170.dp
private val PreviewMinHeight = 200.dp

/** "Escrever | Prévia". */
enum class FormTab { WRITE, PREVIEW }

/** A snippet the editor's buttons insert at the cursor; a [block] goes on its own line. */
data class EditorSnippet(val label: String, val text: String, val block: Boolean, val cursorBack: Int = 0)

/**
 * The form's frame: the fields scroll, the save bar stays at the bottom with "Falta: …" over the button. The whole
 * frame sits above the keyboard.
 */
@Composable
fun SongFormScaffold(
    innerPadding: PaddingValues,
    saveLabel: String,
    missing: List<String>,
    isSaving: Boolean,
    error: String?,
    onSave: () -> Unit,
    fields: @Composable ColumnScope.() -> Unit,
) {
    ResizeForKeyboard()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = fields,
        )
        FormSaveBar(saveLabel, missing, isSaving, error, onSave)
    }
}

/** The fixed bottom bar: the error, "Falta: …" ([missingPrefix]) and the green pill button. */
@Composable
fun FormSaveBar(
    label: String,
    missing: List<String>,
    isSaving: Boolean,
    error: String?,
    onSave: () -> Unit,
    savingLabel: String = SAVING_LABEL,
    missingPrefix: String = MISSING_PREFIX,
) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = DIVIDER_ALPHA))
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                )
            }
            if (missing.isNotEmpty()) {
                Text(
                    text = missingPrefix + missing.joinToString(", "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                )
            }
            Button(
                onClick = onSave,
                enabled = missing.isEmpty() && !isSaving,
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ipbGreen,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(savingLabel, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(label, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** A tonal field: label on top, the value under it; a [primary] border while focused. */
@Composable
fun FormTextField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    enabled: Boolean = true,
    placeholder: String? = null,
    singleLine: Boolean = true,
    monospace: Boolean = false,
    minHeight: Dp = Dp.Unspecified,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(FieldCorner)
    val base = if (monospace) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
    val textStyle = base.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = if (monospace) FontFamily.Monospace else base.fontFamily,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(2.dp, if (focused) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            if (value.text.isEmpty() && placeholder != null) {
                Text(placeholder, style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                textStyle = textStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                interactionSource = interaction,
                keyboardOptions = keyboardOptions,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).heightIn(min = minHeight),
            )
        }
    }
}

/** [FormTextField] for a plain string; the cursor is kept here, and goes to the end when [value] changes outside. */
@Composable
fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    val shown = if (field.text == value) field else TextFieldValue(value, TextRange(value.length))
    FormTextField(
        label = label,
        value = shown,
        onValueChange = {
            field = it
            onValueChange(it.text)
        },
        enabled = enabled,
        placeholder = placeholder,
        keyboardOptions = keyboardOptions,
    )
}

/** "TOM", "INSTRUMENTO": a small uppercase label over a group. */
@Composable
fun FormLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 2.dp, top = 4.dp),
    )
}

/** A hint under a field. */
@Composable
fun FormHelp(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

/**
 * The song: a search with the matching songs under it, or, once picked, a card with the title, the artist and
 * "Trocar".
 */
@Composable
fun SongPicker(
    query: String,
    results: List<Song>,
    selected: Song?,
    enabled: Boolean,
    onQueryChange: (String) -> Unit,
    onSelect: (Song) -> Unit,
    onClear: () -> Unit,
) {
    if (selected != null) {
        FormLabel(SONG_LABEL)
        Row(
            modifier = Modifier.fillMaxWidth().formCard().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_songs),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = selected.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = selected.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onClear, enabled = enabled) { Text(CHANGE_SONG, fontWeight = FontWeight.SemiBold) }
        }
        return
    }
    FormTextField(
        label = SONG_LABEL,
        value = query,
        onValueChange = onQueryChange,
        enabled = enabled,
        placeholder = SONG_PLACEHOLDER,
    )
    if (query.isBlank()) return
    if (results.isEmpty()) {
        FormHelp(NO_SONG_FOUND)
        return
    }
    Column(Modifier.fillMaxWidth().formCard()) {
        results.take(MAX_SONG_RESULTS).forEachIndexed { index, song ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = DIVIDER_ALPHA))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onSelect(song) }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The 12 tones in a 6-column grid; the chosen one in green. */
@Composable
fun TonePicker(selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    FormLabel(TONE_LABEL)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MUSIC_TONES.chunked(TONE_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { tone ->
                    ChoiceButton(tone, tone == selected, enabled, Modifier.weight(1f)) { onSelect(tone) }
                }
            }
        }
    }
}

/** "Baixo" and "Violão", then "Outro instrumento" for anything else. */
@Composable
fun InstrumentPicker(value: String, enabled: Boolean, onChange: (String) -> Unit) {
    FormLabel(INSTRUMENT_LABEL)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth(),
    ) {
        INSTRUMENTS.forEach { instrument ->
            ChoiceButton(instrument, instrument == value, enabled, Modifier, wide = true) { onChange(instrument) }
        }
    }
    FormTextField(
        label = OTHER_INSTRUMENT_LABEL,
        value = value.takeUnless { it in INSTRUMENTS }.orEmpty(),
        onValueChange = onChange,
        enabled = enabled,
        placeholder = OTHER_INSTRUMENT_PLACEHOLDER,
    )
}

@Composable
private fun ChoiceButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    wide: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(if (selected) ipbGreen else Color.Transparent)
            .border(BorderStroke(1.dp, if (selected) ipbGreen else MaterialTheme.colorScheme.outlineVariant), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (wide) 14.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** "Escrever | Prévia". */
@Composable
fun FormTabs(selected: FormTab, onSelect: (FormTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        listOf(FormTab.WRITE to WRITE_TAB, FormTab.PREVIEW to PREVIEW_TAB).forEach { (tab, label) ->
            val isSelected = tab == selected
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = with(MaterialTheme.colorScheme) { if (isSelected) onSurface else onSurfaceVariant },
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isSelected) {
                            Modifier.shadow(2.dp, RoundedCornerShape(percent = 50))
                        } else {
                            Modifier
                        },
                    )
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (isSelected) MaterialTheme.colorScheme.background else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 9.dp),
            )
        }
    }
}

/** The buttons that insert sections and chords at the cursor. */
@Composable
fun EditorTools(snippets: List<EditorSnippet>, enabled: Boolean, onInsert: (EditorSnippet) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        snippets.forEach { snippet ->
            Text(
                text = snippet.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(enabled = enabled) { onInsert(snippet) }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
            )
        }
    }
}

/** [value] with [snippet] at the cursor; a block gets its own line. */
fun TextFieldValue.insert(snippet: EditorSnippet): TextFieldValue {
    val at = selection.start.coerceIn(0, text.length)
    val inserted = if (snippet.block) {
        val before = if (at > 0 && text[at - 1] != '\n') "\n" else ""
        before + snippet.text + "\n"
    } else {
        snippet.text
    }
    val cursor = at + inserted.length - snippet.cursorBack
    return TextFieldValue(text.substring(0, at) + inserted + text.substring(at), TextRange(cursor))
}

/** The big monospace text field. */
@Composable
fun FormEditor(label: String, value: TextFieldValue, enabled: Boolean, onValueChange: (TextFieldValue) -> Unit) {
    FormTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = false,
        monospace = true,
        minHeight = EditorMinHeight,
    )
}

/** The preview's card. */
@Composable
fun PreviewCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PreviewMinHeight)
            .formCard()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content,
    )
}

@Composable
private fun Modifier.formCard(): Modifier = this
    .shadow(2.dp, RoundedCornerShape(CardCorner))
    .clip(RoundedCornerShape(CardCorner))
    .background(highlightCardColor())
