package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordBlock
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.EditorSnippet
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.EditorTools
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormEditor
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormHelp
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormTab
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormTabs
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.PreviewCard
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.insert

private const val EDITOR_LABEL = "Texto da cifra"
private const val EDITOR_HELP = "Acorde entre colchetes antes da sílaba: [G]Santo. " +
    "Seção numa linha: {verse: Estrofe 1}, {chorus: Refrão}."
private const val NO_TONE = "Sem tom"
private const val NOTHING_YET = "Nada para mostrar ainda."

private val SNIPPETS = listOf(
    EditorSnippet("+ Estrofe", "{verse: Estrofe}", block = true),
    EditorSnippet("+ Refrão", "{chorus: Refrão}", block = true),
    EditorSnippet("+ Intro", "{intro}", block = true),
    EditorSnippet("[ ] Acorde", "[]", block = false, cursorBack = 1),
)

/** The chart's text in the forms, new and edit: "Escrever | Prévia" (`specs/worshiphub/spec.md` §4.2). */
@Composable
fun ChordChartEditor(
    content: String,
    tone: String,
    instrument: String,
    previewBlocks: List<ChordBlock>,
    unknownDirectives: Int,
    enabled: Boolean,
    onContentChange: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(FormTab.WRITE) }
    // The editor keeps its cursor here, so the buttons insert where the user is typing.
    var editor by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(content, TextRange(content.length)))
    }
    FormTabs(selected = tab, onSelect = { tab = it })
    when (tab) {
        FormTab.WRITE -> {
            EditorTools(SNIPPETS, enabled) { snippet ->
                editor = editor.insert(snippet)
                onContentChange(editor.text)
            }
            FormHelp(EDITOR_HELP)
            FormEditor(EDITOR_LABEL, editor, enabled) { value ->
                editor = value
                onContentChange(value.text)
            }
        }
        FormTab.PREVIEW -> ChordPreview(tone, instrument, previewBlocks, unknownDirectives)
    }
}

@Composable
private fun ChordPreview(tone: String, instrument: String, blocks: List<ChordBlock>, unknownDirectives: Int) {
    PreviewCard {
        val header = if (tone.isBlank()) NO_TONE else "Tom $tone"
        FormHelp(listOf(header, instrument.trim()).filter { it.isNotEmpty() }.joinToString(" · "))
        Spacer(Modifier.height(4.dp))
        if (blocks.isEmpty()) FormHelp(NOTHING_YET)
        blocks.forEachIndexed { index, block ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            ChordBlockView(block)
        }
        if (unknownDirectives > 0) {
            Text(
                text     = unknownText(unknownDirectives),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun unknownText(count: Int): String =
    if (count == 1) {
        "1 linha entre { } não foi reconhecida como seção."
    } else {
        "$count linhas entre { } não foram reconhecidas como seção."
    }
