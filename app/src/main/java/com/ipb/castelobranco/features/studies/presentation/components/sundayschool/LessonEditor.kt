package com.ipb.castelobranco.features.studies.presentation.components.sundayschool

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.domain.bible.BibleBookInfo
import com.ipb.castelobranco.features.studies.domain.model.BookFacts
import com.ipb.castelobranco.features.studies.domain.model.EditorLocation
import com.ipb.castelobranco.features.studies.domain.model.LessonBlock
import com.ipb.castelobranco.features.studies.domain.model.LessonContent
import com.ipb.castelobranco.features.studies.domain.model.LessonIcon
import com.ipb.castelobranco.features.studies.domain.model.LessonSection
import com.ipb.castelobranco.features.studies.domain.model.ListItem
import com.ipb.castelobranco.features.studies.domain.model.ListStyle
import com.ipb.castelobranco.features.studies.domain.model.ProofText
import com.ipb.castelobranco.features.studies.presentation.viewmodel.NewBlockKind

/** Everything the editor can change; the screen wires each to the ViewModel. */
@Stable
data class LessonEditorActions(
    val onSectionTitle: (section: Int, title: String) -> Unit,
    val onSectionIcon: (section: Int, icon: LessonIcon?) -> Unit,
    val onMoveSection: (section: Int, delta: Int) -> Unit,
    val onDeleteSection: (section: Int) -> Unit,
    val onAddSection: () -> Unit,
    val onAddBlock: (section: Int, kind: NewBlockKind) -> Unit,
    val onUpdateBlock: (section: Int, block: Int, value: LessonBlock) -> Unit,
    val onMoveBlock: (section: Int, block: Int, delta: Int) -> Unit,
    val onDeleteBlock: (section: Int, block: Int) -> Unit,
)

/**
 * The basic editor (FR-018, FR-019): sections, paragraphs, lists, key verses and their chips are edited; the other
 * block types are drawn as members see them and can only be moved or deleted.
 */
fun LazyListScope.lessonEditor(
    content: LessonContent,
    books: Map<String, BookFacts>,
    errors: Map<EditorLocation, List<String>>,
    catalog: List<BibleBookInfo>,
    style: LessonStyle,
    actions: LessonEditorActions,
) {
    content.sections.forEachIndexed { s, section ->
        item(key = "edit-section-$s") {
            SectionEditor(
                index = s,
                count = content.sections.size,
                section = section,
                errors = errors[EditorLocation.Section(s)].orEmpty(),
                actions = actions,
            )
        }
        section.blocks.forEachIndexed { b, block ->
            item(key = "edit-block-$s-$b") {
                BlockEditor(
                    block = block,
                    canMoveUp = b > 0,
                    canMoveDown = b < section.blocks.lastIndex,
                    errors = errors[EditorLocation.Block(s, b)].orEmpty(),
                    books = books,
                    catalog = catalog,
                    style = style,
                    onChange = { actions.onUpdateBlock(s, b, it) },
                    onMove = { delta -> actions.onMoveBlock(s, b, delta) },
                    onDelete = { actions.onDeleteBlock(s, b) },
                )
            }
        }
        item(key = "edit-add-$s") { AddBlockButton(onAdd = { kind -> actions.onAddBlock(s, kind) }) }
    }
    item(key = "edit-add-section") {
        TextButton(onClick = actions.onAddSection, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("Adicionar seção", modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun SectionEditor(
    index: Int,
    count: Int,
    section: LessonSection,
    errors: List<String>,
    actions: LessonEditorActions,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SEÇÃO ${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                MoveDeleteMenu(
                    canMoveUp = index > 0,
                    canMoveDown = index < count - 1,
                    deleteLabel = "Apagar seção",
                    onMove = { actions.onMoveSection(index, it) },
                    onDelete = { actions.onDeleteSection(index) },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconPicker(selected = section.icon, onSelect = { actions.onSectionIcon(index, it) })
                OutlinedTextField(
                    value = section.title,
                    onValueChange = { actions.onSectionTitle(index, it) },
                    label = { Text("Título da seção") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            ErrorLines(errors)
        }
    }
}

@Composable
private fun IconPicker(selected: LessonIcon?, onSelect: (LessonIcon?) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedCard(onClick = { expanded = true }) {
            Box(Modifier.padding(12.dp)) {
                if (selected != null) {
                    LessonIconImage(icon = selected, tint = MaterialTheme.colorScheme.primary, size = 28.dp)
                } else {
                    Icon(Icons.Outlined.Block, contentDescription = "Sem ícone", modifier = Modifier.padding(2.dp))
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Sem ícone") },
                leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            LessonIcon.entries.forEach { icon ->
                DropdownMenuItem(
                    text = { Text(icon.label) },
                    leadingIcon = { LessonIconImage(icon = icon, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        onSelect(icon)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun BlockEditor(
    block: LessonBlock,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    errors: List<String>,
    books: Map<String, BookFacts>,
    catalog: List<BibleBookInfo>,
    style: LessonStyle,
    onChange: (LessonBlock) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        border = BorderStroke(
            1.dp,
            if (errors.isEmpty()) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.error,
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = block.typeLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                MoveDeleteMenu(
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    deleteLabel = "Apagar bloco",
                    onMove = onMove,
                    onDelete = onDelete,
                )
            }
            when (block) {
                is LessonBlock.Paragraph -> ParagraphEditor(block, catalog, onChange)
                is LessonBlock.BulletList -> ListEditor(block, catalog, onChange)
                is LessonBlock.KeyVerse -> KeyVerseEditor(block, catalog, onChange)
                else -> ReadOnlyBlock(block, books, style)
            }
            ErrorLines(errors)
        }
    }
}

@Composable
private fun ParagraphEditor(
    block: LessonBlock.Paragraph,
    catalog: List<BibleBookInfo>,
    onChange: (LessonBlock) -> Unit,
) {
    OutlinedTextField(
        value = block.text,
        onValueChange = { onChange(block.copy(text = it)) },
        label = { Text("Texto") },
        supportingText = { Text(MARKUP_HINT) },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
    ProofTextsEditor(block.proofTexts, catalog) { onChange(block.copy(proofTexts = it)) }
}

@Composable
private fun ListEditor(
    block: LessonBlock.BulletList,
    catalog: List<BibleBookInfo>,
    onChange: (LessonBlock) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ListStyle.entries.forEach { style ->
            FilterChip(
                selected = block.style == style,
                onClick = { onChange(block.copy(style = style)) },
                label = { Text(style.label()) },
            )
        }
    }
    block.items.forEachIndexed { index, item ->
        fun replace(value: ListItem) =
            onChange(block.copy(items = block.items.mapIndexed { i, it -> if (i == index) value else it }))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                OutlinedTextField(
                    value = item.text,
                    onValueChange = { replace(item.copy(text = it)) },
                    label = { Text("Item ${index + 1}") },
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onChange(block.copy(items = block.items.filterIndexed { i, _ -> i != index })) },
                    enabled = block.items.size > 1,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Remover item")
                }
            }
            ProofTextsEditor(item.proofTexts, catalog) { replace(item.copy(proofTexts = it)) }
        }
    }
    TextButton(onClick = { onChange(block.copy(items = block.items + ListItem(""))) }) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Text("Adicionar item", modifier = Modifier.padding(start = 6.dp))
    }
    Text(MARKUP_HINT, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun KeyVerseEditor(
    block: LessonBlock.KeyVerse,
    catalog: List<BibleBookInfo>,
    onChange: (LessonBlock) -> Unit,
) {
    OutlinedTextField(
        value = block.label.orEmpty(),
        onValueChange = { onChange(block.copy(label = it.ifBlank { null })) },
        label = { Text("Rótulo (opcional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = block.text,
        onValueChange = { onChange(block.copy(text = it)) },
        label = { Text("Texto do versículo") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    var editing by rememberSaveable { mutableStateOf(false) }
    Text("Referência", style = MaterialTheme.typography.labelMedium)
    if (block.proof.printed.isBlank()) {
        AssistChip(
            onClick = { editing = true },
            label = { Text("Adicionar referência") },
            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
        )
    } else {
        InputChip(selected = false, onClick = { editing = true }, label = { Text(block.proof.printed) })
    }
    if (editing) {
        ProofTextEditorDialog(
            initial = block.proof.takeIf { it.printed.isNotBlank() },
            books = catalog,
            onConfirm = {
                onChange(block.copy(proof = it))
                editing = false
            },
            onDelete = null,
            onDismiss = { editing = false },
        )
    }
}

/** The chips of a paragraph or list item: tap to edit, "+" to add. */
@Composable
private fun ProofTextsEditor(
    proofTexts: List<ProofText>,
    catalog: List<BibleBookInfo>,
    onChange: (List<ProofText>) -> Unit,
) {
    /** Index being edited; [proofTexts].size = a new chip. */
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        proofTexts.forEachIndexed { index, proof ->
            InputChip(selected = false, onClick = { editing = index }, label = { Text(proof.printed) })
        }
        AssistChip(
            onClick = { editing = proofTexts.size },
            label = { Text("Referência") },
            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
        )
    }
    val index = editing ?: return
    val existing = proofTexts.getOrNull(index)
    ProofTextEditorDialog(
        initial = existing,
        books = catalog,
        onConfirm = { proof ->
            val updated = if (existing == null) {
                proofTexts + proof
            } else {
                proofTexts.mapIndexed { i, it -> if (i == index) proof else it }
            }
            onChange(updated)
            editing = null
        },
        onDelete = existing?.let {
            {
                onChange(proofTexts.filterIndexed { i, _ -> i != index })
                editing = null
            }
        },
        onDismiss = { editing = null },
    )
}

@Composable
private fun ReadOnlyBlock(block: LessonBlock, books: Map<String, BookFacts>, style: LessonStyle) {
    Text(
        text = "Este bloco só pode ser movido ou apagado",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (block is LessonBlock.Unknown) return
    val preview = remember(style) { style.copy(fontSize = style.fontSize * READ_ONLY_SCALE) }
    Box(Modifier.fillMaxWidth()) {
        LessonBlockView(block = block, books = books, style = preview, onReferenceClick = {})
    }
}

@Composable
private fun AddBlockButton(onAdd: (NewBlockKind) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("Adicionar bloco", modifier = Modifier.padding(start = 6.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            NewBlockKind.entries.forEach { kind ->
                DropdownMenuItem(
                    text = { Text(kind.label()) },
                    onClick = {
                        onAdd(kind)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MoveDeleteMenu(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    deleteLabel: String,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Opções") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Mover para cima") },
                enabled = canMoveUp,
                onClick = {
                    onMove(-1)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text("Mover para baixo") },
                enabled = canMoveDown,
                onClick = {
                    onMove(1)
                    expanded = false
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(deleteLabel, color = MaterialTheme.colorScheme.error) },
                onClick = {
                    onDelete()
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun ErrorLines(errors: List<String>) {
    errors.forEach {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

private fun LessonBlock.typeLabel(): String = when (this) {
    is LessonBlock.Paragraph -> "Parágrafo"
    is LessonBlock.BulletList -> "Lista"
    is LessonBlock.KeyVerse -> "Versículo-chave"
    is LessonBlock.Cards -> "Cartões"
    is LessonBlock.Comparison -> "Comparação"
    is LessonBlock.Steps -> "Etapas"
    is LessonBlock.Table -> "Tabela"
    is LessonBlock.Stats -> "Números"
    is LessonBlock.Timeline -> "Linha do tempo"
    is LessonBlock.BookFactsBlock -> "Ficha do livro"
    is LessonBlock.Unknown -> "Bloco não suportado nesta versão"
}

private fun ListStyle.label(): String = when (this) {
    ListStyle.BULLET -> "• Marcadores"
    ListStyle.NUMBERED -> "1. Números"
    ListStyle.ROMAN -> "I. Romanos"
}

private fun NewBlockKind.label(): String = when (this) {
    NewBlockKind.PARAGRAPH -> "Parágrafo"
    NewBlockKind.LIST -> "Lista"
    NewBlockKind.KEY_VERSE -> "Versículo-chave"
}

private const val MARKUP_HINT = "**negrito** e *itálico*"
private const val READ_ONLY_SCALE = 0.85f
