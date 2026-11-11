package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.screens

import com.ipb.castelobranco.core.presentation.components.MenuAction
import com.ipb.castelobranco.core.presentation.components.OverflowMenu
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import com.ipb.castelobranco.core.data.local.SongScrollMode
import com.ipb.castelobranco.core.presentation.modifier.tapToPaginate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.components.ChordBlockView
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordBlock
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.state.ChordChartDetailUiState
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.viewmodel.ChordChartDetailViewModel
import com.ipb.castelobranco.core.presentation.components.KeepScreenOn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.ipb.castelobranco.core.presentation.components.isCompactHeight
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.CancelBarAction
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongFormScaffold
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongFormSaveAction
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongSummary
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.InstrumentPicker
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.TonePicker
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.components.ChordChartEditor
import androidx.compose.foundation.layout.PaddingValues
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.PageProgress
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongBand
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.TextSizeRow
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.ToneTag

private const val EDIT_LABEL = "Editar cifra"
private const val SAVE_LABEL = "Salvar cifra"

/** A tap on the right half turns the page, on the left half goes back. */
private const val HALF_SCREEN = 0.5f
private const val LANDSCAPE_PAGE_COLUMNS = 2
private val PageColumnGap = 28.dp

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
        onTextScaleChange  = viewModel::setTextScale,
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
    onTextScaleChange: (Int) -> Unit,
    onBackClick: () -> Unit,
    onEnterEdit: () -> Unit,
    onEditContentChange: (String) -> Unit,
    onEditToneChange: (String) -> Unit,
    onEditInstrumentChange: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
) {
    BaseScreen(
        tabName       = if (state.isEditing) EDIT_LABEL else state.songName.ifEmpty { "Cifra" },
        logoRes       = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick   = onBackClick,
        extraActions  = {
            when {
                state.isEditing -> {
                    SongFormSaveAction(SAVE_LABEL, state.editMissing, state.isSaving, onSaveEdit)
                    CancelBarAction(state.isSaving, onCancelEdit)
                }
                state.blocks.isNotEmpty() -> OverflowMenu(
                    actions = listOfNotNull(
                        MenuAction(Icons.Filled.Edit, EDIT_LABEL, onEnterEdit).takeIf { state.canEdit },
                    ),
                    controls = { TextSizeRow(percent = state.textScalePercent, onPercentChange = onTextScaleChange) },
                )
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
    // Lying down each page shows its pieces in columns, side by side (`specs/worshiphub/spec.md` §4).
    val columns = if (isCompactHeight()) LANDSCAPE_PAGE_COLUMNS else 1
    SubcomposeLayout(modifier = modifier.fillMaxSize()) { constraints ->
        val blockSpacingPx = 16.dp.roundToPx()
        // Matches ChordPageContent padding(horizontal = 20.dp, vertical = 12.dp) and its column gap
        val hPaddingPx = 40.dp.roundToPx()
        val vPaddingPx = 24.dp.roundToPx()
        val gapsPx = PageColumnGap.roundToPx() * (columns - 1)
        val measureWidth = ((constraints.maxWidth - hPaddingPx - gapsPx) / columns).coerceAtLeast(0)

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

        // Phase 3: compute the columns with the actual available height, then put [columns] of them on each page
        val pages = BlockPaginator.paginate(blocks, blockHeights, availableForBlocks, blockSpacingPx)
            .chunked(columns)

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

/** [pages]: each page's columns, each column's pieces. */
@Composable
private fun PagerContent(
    pages: List<List<List<PagePiece>>>,
    textScale: Float,
    anchor: PiecePosition,
    onAnchor: (PiecePosition) -> Unit,
    band: @Composable () -> Unit,
) {
    val flatPages = remember(pages) { pages.map { it.flatten() } }
    val pagerState = rememberPagerState(initialPage = BlockPaginator.pageOf(flatPages, anchor)) { pages.size }
    val scope = rememberCoroutineScope()

    // New pages (text size, rotation): back to the page with the anchor, then follow the reader again.
    LaunchedEffect(pages) {
        val target = BlockPaginator.pageOf(flatPages, anchor)
        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
        snapshotFlow { pagerState.settledPage }.collect { page ->
            flatPages.getOrNull(page)?.firstOrNull()?.let { onAnchor(it.position) }
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
                ChordPageContent(columns = pages[index], textScale = textScale)
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
private fun ChordPageContent(columns: List<List<PagePiece>>, textScale: Float) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(PageColumnGap),
    ) {
        columns.forEach { pieces ->
            // Long press selects the text to copy; taps still turn the page.
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Column {
                    pieces.forEachIndexed { index, piece ->
                        if (index > 0) Spacer(modifier = Modifier.height(16.dp))
                        ChordBlockView(piece.block, textScale)
                    }
                }
            }
        }
        // A last page with fewer columns keeps them the same width.
        if (columns.size < LANDSCAPE_PAGE_COLUMNS && isCompactHeight()) {
            repeat(LANDSCAPE_PAGE_COLUMNS - columns.size) { Spacer(Modifier.weight(1f)) }
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
        editor       = {
            ChordChartEditor(
                content           = state.editContent,
                tone              = state.editTone,
                instrument        = state.editInstrument,
                previewBlocks     = state.editPreviewBlocks,
                unknownDirectives = state.editUnknownDirectives,
                enabled           = !state.isSaving,
                onContentChange   = onContentChange,
            )
        },
    ) {
        val enabled = !state.isSaving
        SongSummary(title = state.songName, detail = state.artist)
        TonePicker(selected = state.editTone, enabled = enabled, onSelect = onToneChange)
        InstrumentPicker(value = state.editInstrument, enabled = enabled, onChange = onInstrumentChange)
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
