package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.parser.LyricsStanza
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormEditor
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormHelp
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormTab
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormTabs
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.PreviewCard

private const val EDITOR_LABEL = "Texto da letra"
private const val EDITOR_HELP = "Linha em branco separa as estrofes."
private const val NOTHING_YET = "Nada para mostrar ainda."

/** The lyrics' text in the forms, new and edit: "Escrever | Prévia" (`specs/worshiphub/spec.md` §5.2). */
@Composable
fun LyricsEditor(
    content: String,
    previewStanzas: List<LyricsStanza>,
    enabled: Boolean,
    onContentChange: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(FormTab.WRITE) }
    var editor by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(content, TextRange(content.length)))
    }
    FormTabs(selected = tab, onSelect = { tab = it })
    when (tab) {
        FormTab.WRITE -> {
            FormHelp(EDITOR_HELP)
            FormEditor(EDITOR_LABEL, editor, enabled) { value ->
                editor = value
                onContentChange(value.text)
            }
        }
        FormTab.PREVIEW -> PreviewCard {
            if (previewStanzas.isEmpty()) FormHelp(NOTHING_YET)
            previewStanzas.forEachIndexed { index, stanza ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                stanza.lines.forEach { line ->
                    Text(line, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
