package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import com.ipb.castelobranco.core.data.local.SongScrollMode
import com.ipb.castelobranco.core.presentation.modifier.tapToPaginate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.components.ChordBlockView
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordBlock
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordLine
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.LineToken
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.state.ChordChartDetailUiState
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.viewmodel.ChordChartDetailViewModel
import com.ipb.castelobranco.core.presentation.components.KeepScreenOn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import com.ipb.castelobranco.core.presentation.components.PanelAction
import com.ipb.castelobranco.core.presentation.components.PanelHeader
import com.ipb.castelobranco.core.presentation.components.PanelMoreButton
import com.ipb.castelobranco.core.presentation.components.TopActionPanel
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.CancelBarAction
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongFormScaffold
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongSummary
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.InstrumentPicker
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.TonePicker
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.components.ChordChartEditor
import androidx.compose.foundation.layout.PaddingValues
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.PageProgress
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SectionTag
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongBand
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.ToneTag
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.chordColor

private const val EDIT_LABEL = "Editar cifra"
private const val EDIT_HINT = "Muda o tom, o instrumento e o texto"
private const val SAVE_LABEL = "Salvar cifra"
private const val TEXT_SIZE_LABEL = "Tamanho da letra"
private const val SMALLER_TEXT_LABEL = "A−"
private const val BIGGER_TEXT_LABEL = "A+"

/** A tap on the right half turns the page, on the left half goes back. */
private const val HALF_SCREEN = 0.5f

@Composable
fun ChordChartDetailScreen(
    viewModel: ChordChartDetailViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollMode by viewModel.scrollMode.collectAsStateWithLifecycle()
    KeepScreenOn()
    ChordChartDetailContent(
        state              = state,
        scrollMode         = scrollMode,
        onToggleScroll     = viewModel::toggleScrollMode,
        onShrinkText       = viewModel::shrinkText,
        onGrowText         = viewModel::growText,
        onBackClick        = onBackClick,
        onEnterEdit        = viewModel::enterEditMode,
        onEditContentChange = viewModel::onEditContentChange,
        onEditToneChange   = viewModel::onEditToneChange,
        onEditInstrumentChange = viewModel::onEditInstrumentChange,
        onCancelEdit       = viewModel::cancelEdit,
        onSaveEdit         = viewModel::saveEdit,
    )
}

@Composable
private fun ChordChartDetailContent(
    state: ChordChartDetailUiState,
    scrollMode: SongScrollMode?,
    onToggleScroll: () -> Unit,
    onShrinkText: () -> Unit,
    onGrowText: () -> Unit,
    onBackClick: () -> Unit,
    onEnterEdit: () -> Unit,
    onEditContentChange: (String) -> Unit,
    onEditToneChange: (String) -> Unit,
    onEditInstrumentChange: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
) {
    var isPanelOpen by rememberSaveable { mutableStateOf(false) }
    BaseScreen(
        tabName       = if (state.isEditing) EDIT_LABEL else state.songName.ifEmpty { "Cifra" },
        logoRes       = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick   = onBackClick,
        extraActions  = {
            when {
                state.isEditing -> CancelBarAction(state.isSaving, onCancelEdit)
                else -> PanelMoreButton(hasActions = state.blocks.isNotEmpty(), onClick = { isPanelOpen = true })
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> LoadingState(Modifier.padding(innerPadding))
                state.error != null -> ErrorState(state.error, Modifier.padding(innerPadding))
                state.isEditing -> EditContent(
                    state              = state,
                    innerPadding       = innerPadding,
                    onContentChange    = onEditContentChange,
                    onToneChange       = onEditToneChange,
                    onInstrumentChange = onEditInstrumentChange,
                    onSave             = onSaveEdit,
                )
                state.blocks.isEmpty() -> ErrorState("Sem conteúdo disponível", Modifier.padding(innerPadding))
                // Opens straight in the saved mode: nothing is drawn until it is known.
                scrollMode == null -> LoadingState(Modifier.padding(innerPadding))
                scrollMode == SongScrollMode.VERTICAL -> ChordVerticalContent(
                    blocks   = state.blocks,
                    textScale = state.textScale,
                    band     = { ChordBand(state, scrollMode, onToggleScroll) },
                    modifier = Modifier.padding(innerPadding),
                )
                else -> ChordPager(
                    blocks   = state.blocks,
                    textScale = state.textScale,
                    band     = { ChordBand(state, scrollMode, onToggleScroll) },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            Box(Modifier.padding(innerPadding)) {
                TopActionPanel(
                    visible = isPanelOpen && !state.isEditing,
                    actions = if (state.canEdit) {
                        listOf(PanelAction(Icons.Filled.Edit, EDIT_LABEL, onEnterEdit, EDIT_HINT))
                    } else {
                        emptyList()
                    },
                    onDismiss = { isPanelOpen = false },
                    header = PanelHeader(
                        name = state.songName,
                        detail = listOfNotNull(
                            state.artist.takeIf { it.isNotBlank() },
                            state.tone.takeIf { it.isNotBlank() }?.let { "Tom $it" },
                        ).joinToString(" · "),
                    ),
                    controls = {
                        TextSizeRow(
                            percent   = state.textScalePercent,
                            canShrink = state.canShrinkText,
                            canGrow   = state.canGrowText,
                            onShrink  = onShrinkText,
                            onGrow    = onGrowText,
                        )
                    },
                )
            }
        }
    }
}

/**
 * Measures each block's height, computes page splits via [BlockPaginator], then subcomposes
 * the entire pager UI (including [rememberPagerState]) with the final pages already known.
 * This avoids any state round-trip between the measurement layout and the pager.
 */
@Composable
private fun ChordPager(
    blocks: List<ChordBlock>,
    textScale: Float,
    band: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The piece at the top of the page being read: kept on screen when the pages change.
    var anchor by remember { mutableStateOf(PiecePosition(0, 0)) }
    SubcomposeLayout(modifier = modifier.fillMaxSize()) { constraints ->
        val blockSpacingPx = 16.dp.roundToPx()
        // Matches ChordPageContent padding(horizontal = 20.dp, vertical = 12.dp)
        val hPaddingPx = 40.dp.roundToPx()
        val vPaddingPx = 24.dp.roundToPx()
        val measureWidth = (constraints.maxWidth - hPaddingPx).coerceAtLeast(0)

        // Phase 1: measure chrome (header + dots) to determine pager area height
        val headerHeight = subcompose("chrome_header") {
            band()
        }.first().measure(Constraints(maxWidth = constraints.maxWidth)).height

        val dotsHeight = subcompose("chrome_dots") {
            PageProgress(currentPage = 0, pageCount = 1)
        }.first().measure(Constraints(maxWidth = constraints.maxWidth)).height

        val availableForBlocks = (constraints.maxHeight - headerHeight - dotsHeight - vPaddingPx)
            .coerceAtLeast(1)

        // Phase 2: measure each block at the correct render width. Column wrapper is required —
        // ChordBlockView emits multiple root nodes (Text, Spacers, Rows) with no container, so
        // .firstOrNull() without it would only measure the first child.
        fun measure(slot: String, block: ChordBlock): Int = subcompose(slot) {
            Column { ChordBlockView(block, textScale) }
        }.firstOrNull()?.measure(Constraints(maxWidth = measureWidth))?.height ?: 0

        // A block taller than the page also gets its tag and each line measured, to go on in the next pages.
        val blockHeights = blocks.mapIndexed { i, block ->
            val total = measure("m_$i", block)
            if (total <= availableForBlocks || block.isIntro || block.lines.size < 2) {
                BlockHeights(total)
            } else {
                val continuationHead = measure("c_$i", block.copy(title = null, lines = emptyList()))
                BlockHeights(
                    total            = total,
                    head             = measure("h_$i", block.copy(lines = emptyList())),
                    continuationHead = continuationHead,
                    lines            = block.lines.mapIndexed { k, line ->
                        measure("l_${i}_$k", block.copy(title = null, lines = listOf(line))) - continuationHead
                    },
                )
            }
        }

        // Phase 3: compute pages with the actual available height for block content
        val pages = BlockPaginator.paginate(blocks, blockHeights, availableForBlocks, blockSpacingPx)

        // Phase 4: render the full pager — pagerState lives inside this subcomposition
        val contentPlaceable = subcompose("pager") {
            PagerContent(
                pages     = pages,
                textScale = textScale,
                anchor    = anchor,
                onAnchor  = { anchor = it },
                band      = band,
            )
        }.first().measure(constraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceable.place(0, 0)
        }
    }
}

@Composable
private fun PagerContent(
    pages: List<List<PagePiece>>,
    textScale: Float,
    anchor: PiecePosition,
    onAnchor: (PiecePosition) -> Unit,
    band: @Composable () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = BlockPaginator.pageOf(pages, anchor)) { pages.size }
    val scope = rememberCoroutineScope()

    // New pages (text size, rotation): back to the page with the anchor, then follow the reader again.
    LaunchedEffect(pages) {
        val target = BlockPaginator.pageOf(pages, anchor)
        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
        snapshotFlow { pagerState.settledPage }.collect { page ->
            pages.getOrNull(page)?.firstOrNull()?.let { onAnchor(it.position) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        band()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .tapToPaginate(pagerState, scope, zoneWidthFraction = HALF_SCREEN),
        ) {
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                ChordPageContent(pieces = pages[index], textScale = textScale)
            }
        }

        PageProgress(currentPage = pagerState.currentPage, pageCount = pages.size)
    }
}

@Composable
private fun ChordVerticalContent(
    blocks: List<ChordBlock>,
    textScale: Float,
    band: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        band()

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            SelectionContainer {
                Column {
                    blocks.forEachIndexed { index, block ->
                        if (index > 0) Spacer(modifier = Modifier.height(16.dp))
                        ChordBlockView(block, textScale)
                    }
                }
            }
        }
    }
}

/** The thin band: artist, "Tom D" and the pages/scroll switch. */
@Composable
private fun ChordBand(state: ChordChartDetailUiState, scrollMode: SongScrollMode, onToggleScroll: () -> Unit) {
    SongBand(artist = state.artist, scrollMode = scrollMode, onToggleScroll = onToggleScroll) {
        if (state.tone.isNotBlank()) ToneTag(state.tone)
    }
}

@Composable
private fun ChordPageContent(pieces: List<PagePiece>, textScale: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        // Long press selects the text to copy; taps still turn the page.
        SelectionContainer {
            Column {
                pieces.forEachIndexed { index, piece ->
                    if (index > 0) Spacer(modifier = Modifier.height(16.dp))
                    ChordBlockView(piece.block, textScale)
                }
            }
        }
    }
}

/** "Tamanho da letra  A−  100%  A+" in the three dots' panel; it stays open while the size changes. */
@Composable
private fun TextSizeRow(percent: Int, canShrink: Boolean, canGrow: Boolean, onShrink: () -> Unit, onGrow: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(TEXT_SIZE_LABEL, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onShrink, enabled = canShrink, contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text(SMALLER_TEXT_LABEL, fontWeight = FontWeight.SemiBold)
        }
        Text("$percent%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = onGrow, enabled = canGrow, contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text(BIGGER_TEXT_LABEL, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** The edit form: the new chart's fields, with the song read-only (`specs/worshiphub/spec.md` §4.3). */
@Composable
private fun EditContent(
    state: ChordChartDetailUiState,
    innerPadding: PaddingValues,
    onContentChange: (String) -> Unit,
    onToneChange: (String) -> Unit,
    onInstrumentChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    SongFormScaffold(
        innerPadding = innerPadding,
        saveLabel    = SAVE_LABEL,
        missing      = state.editMissing,
        isSaving     = state.isSaving,
        error        = state.saveError,
        onSave       = onSave,
    ) {
        val enabled = !state.isSaving
        SongSummary(title = state.songName, detail = state.artist)
        TonePicker(selected = state.editTone, enabled = enabled, onSelect = onToneChange)
        InstrumentPicker(value = state.editInstrument, enabled = enabled, onChange = onInstrumentChange)
        ChordChartEditor(
            content           = state.editContent,
            tone              = state.editTone,
            instrument        = state.editInstrument,
            previewBlocks     = state.editPreviewBlocks,
            unknownDirectives = state.editUnknownDirectives,
            enabled           = enabled,
            onContentChange   = onContentChange,
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text  = "Carregando...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
