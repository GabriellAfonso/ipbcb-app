package com.ipb.castelobranco.features.studies.presentation.components.sundayschool

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.domain.bible.BibleBookInfo
import com.ipb.castelobranco.features.studies.domain.model.BibleReference
import com.ipb.castelobranco.features.studies.domain.model.ProofText
import com.ipb.castelobranco.features.studies.domain.model.VerseRangesInput

/**
 * Edits one reference chip: the printed text, and optionally the passage it opens (book, chapter, verses). The
 * passage is checked against the Bible's book list when it is on the device (research R-11).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProofTextEditorDialog(
    initial: ProofText?,
    books: List<BibleBookInfo>,
    onConfirm: (ProofText) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val reference = initial?.reference
    var printed by rememberSaveable { mutableStateOf(initial?.printed.orEmpty()) }
    var opensBible by rememberSaveable { mutableStateOf(initial == null || reference != null) }
    var book by rememberSaveable { mutableStateOf(reference?.book.orEmpty()) }
    var chapter by rememberSaveable { mutableStateOf(reference?.chapter?.toString().orEmpty()) }
    var verses by rememberSaveable { mutableStateOf(reference?.let { VerseRangesInput.format(it.verses) }.orEmpty()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova referência" else "Referência") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = printed,
                    onValueChange = { printed = it },
                    label = { Text("Texto do chip") },
                    placeholder = { Text("Ex.: Daniel 1.8") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Abre na Bíblia", modifier = Modifier.weight(1f))
                    Switch(checked = opensBible, onCheckedChange = { opensBible = it })
                }
                if (opensBible) {
                    BookField(book = book, books = books, onBookChange = { book = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = chapter,
                            onValueChange = { value -> chapter = value.filter { it.isDigit() }.take(CHAPTER_DIGITS) },
                            label = { Text("Capítulo") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = verses,
                            onValueChange = { verses = it },
                            label = { Text("Versículos") },
                            placeholder = { Text("1-3,5") },
                            singleLine = true,
                            modifier = Modifier.weight(1.4f),
                        )
                    }
                    Text(
                        text = "Versículos em branco: o capítulo inteiro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = buildProofText(printed, opensBible, book, chapter, verses, books)
                result.fold(onSuccess = onConfirm, onFailure = { error = it.message })
            }) { Text("OK") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Remover") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookField(book: String, books: List<BibleBookInfo>, onBookChange: (String) -> Unit) {
    if (books.isEmpty()) {
        OutlinedTextField(
            value = book,
            onValueChange = onBookChange,
            label = { Text("Livro (abreviação)") },
            placeholder = { Text("Ex.: Dn, 1Tm") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selected = books.firstOrNull { it.abbrev.equals(book, ignoreCase = true) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: book,
            onValueChange = {},
            readOnly = true,
            label = { Text("Livro") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            books.forEach { info ->
                DropdownMenuItem(
                    text = { Text(info.name) },
                    onClick = {
                        onBookChange(info.abbrev)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** The chip, or why it cannot be saved. */
internal fun buildProofText(
    printed: String,
    opensBible: Boolean,
    book: String,
    chapter: String,
    verses: String,
    books: List<BibleBookInfo>,
): Result<ProofText> {
    val text = printed.trim()
    if (text.isEmpty()) return Result.failure(IllegalArgumentException("Informe o texto do chip"))
    if (!opensBible) return Result.success(ProofText(text, null))
    val abbrev = book.trim()
    if (abbrev.isEmpty()) return Result.failure(IllegalArgumentException("Escolha o livro"))
    val info = books.firstOrNull { it.abbrev.equals(abbrev, ignoreCase = true) }
    val chapterNumber = chapter.toIntOrNull()?.takeIf { it >= 1 }
        ?: return Result.failure(IllegalArgumentException("Informe o capítulo"))
    if (info != null && chapterNumber > info.chapterCount) {
        return Result.failure(IllegalArgumentException("${info.name} tem ${info.chapterCount} capítulos"))
    }
    val spans = VerseRangesInput.parse(verses)
        ?: return Result.failure(IllegalArgumentException("Versículos como 1-3,5"))
    val last = spans.maxOfOrNull { it.end } ?: 0
    val verseCount = info?.verseCount(chapterNumber)
    if (verseCount != null && last > verseCount) {
        return Result.failure(IllegalArgumentException("${info.name} $chapterNumber tem $verseCount versículos"))
    }
    return Result.success(ProofText(text, BibleReference(info?.abbrev ?: abbrev, chapterNumber, spans)))
}

private const val CHAPTER_DIGITS = 3
