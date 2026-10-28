package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import com.ipb.castelobranco.core.presentation.components.PanelAction
import com.ipb.castelobranco.core.presentation.components.PanelHeader
import com.ipb.castelobranco.core.presentation.components.PanelMoreButton
import com.ipb.castelobranco.core.presentation.components.TopActionPanel
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.EditBarActions
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.PageProgress
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SectionTag
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongBand
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.ToneTag
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.chordColor

private const val EDIT_LABEL = "Editar cifra"
private const val EDIT_HINT = "Muda o texto e os acordes"

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
        onBackClick        = onBackClick,
        onEnterEdit        = viewModel::enterEditMode,
        onEditContentChange = viewModel::onEditContentChange,
        onCancelEdit       = viewModel::cancelEdit,
        onSaveEdit         = viewModel::saveEdit,
    )
}

@Composable
private fun ChordChartDetailContent(
    state: ChordChartDetailUiState,
    scrollMode: SongScrollMode?,
    onToggleScroll: () -> Unit,
    onBackClick: () -> Unit,
    onEnterEdit: () -> Unit,
    onEditContentChange: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
) {
    var isPanelOpen by rememberSaveable { mutableStateOf(false) }
    BaseScreen(
        tabName       = state.songName.ifEmpty { "Cifra" },
        logoRes       = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick   = onBackClick,
        extraActions  = {
            when {
                state.isEditing -> EditBarActions(state.isSaving, onCancel = onCancelEdit, onSave = onSaveEdit)
                state.canEdit -> PanelMoreButton(hasActions = true, onClick = { isPanelOpen = true })
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> LoadingState(Modifier.padding(innerPadding))
                state.error != null -> ErrorState(state.error, Modifier.padding(innerPadding))
                state.isEditing -> EditContent(
                    editContent     = state.editContent,
                    isSaving        = state.isSaving,
                    saveError       = state.saveError,
                    onContentChange = onEditContentChange,
                    modifier        = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .imePadding(),
                )
                state.blocks.isEmpty() -> ErrorState("Sem conteúdo disponível", Modifier.padding(innerPadding))
                // Opens straight in the saved mode: nothing is drawn until it is known.
                scrollMode == null -> LoadingState(Modifier.padding(innerPadding))
                scrollMode == SongScrollMode.VERTICAL -> ChordVerticalContent(
                    blocks         = state.blocks,
                    tone           = state.tone,
                    artist         = state.artist,
                    scrollMode     = scrollMode,
                    onToggleScroll = onToggleScroll,
                    modifier       = Modifier.padding(innerPadding),
                )
                else -> ChordPager(
                    blocks         = state.blocks,
                    tone           = state.tone,
                    artist         = state.artist,
                    scrollMode     = scrollMode,
                    onToggleScroll = onToggleScroll,
                    modifier       = Modifier.padding(innerPadding),
                )
            }
            Box(Modifier.padding(innerPadding)) {
                TopActionPanel(
                    visible = isPanelOpen && state.canEdit && !state.isEditing,
                    actions = listOf(PanelAction(Icons.Filled.Edit, EDIT_LABEL, onEnterEdit, EDIT_HINT)),
                    onDismiss = { isPanelOpen = false },
                    header = PanelHeader(
                        name = state.songName,
                        detail = listOfNotNull(
                            state.artist.takeIf { it.isNotBlank() },
                            state.tone.takeIf { it.isNotBlank() }?.let { "Tom $it" },
                        ).joinToString(" · "),
                    ),
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
    tone: String,
    artist: String,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier = modifier.fillMaxSize()) { constraints ->
        val blockSpacingPx = 16.dp.roundToPx()
        // Matches ChordPageContent padding(horizontal = 20.dp, vertical = 12.dp)
        val hPaddingPx = 40.dp.roundToPx()
        val vPaddingPx = 24.dp.roundToPx()
        val measureWidth = (constraints.maxWidth - hPaddingPx).coerceAtLeast(0)

        // Phase 1: measure each block at the correct render width.
        // Column wrapper is required — ChordBlockView emits multiple root nodes (Text, Spacers, Rows)
        // with no container, so .firstOrNull() without it would only measure the first child.
        val blockHeights = blocks.mapIndexed { i, block ->
            subcompose("m_$i") {
                Column {
                    ChordBlockView(block)
                }
            }.firstOrNull()
                ?.measure(Constraints(maxWidth = measureWidth))
                ?.height ?: 0
        }

        // Phase 2: measure chrome (header + dots) to determine pager area height
        val headerHeight = subcompose("chrome_header") {
            ChordBand(artist = artist, tone = tone, scrollMode = scrollMode, onToggleScroll = {})
        }.first().measure(Constraints(maxWidth = constraints.maxWidth)).height

        val dotsHeight = subcompose("chrome_dots") {
            PageProgress(currentPage = 0, pageCount = 1)
        }.first().measure(Constraints(maxWidth = constraints.maxWidth)).height

        // Phase 3: compute pages with the actual available height for block content
        val availableForBlocks = (constraints.maxHeight - headerHeight - dotsHeight - vPaddingPx)
            .coerceAtLeast(1)
        val pages = BlockPaginator.paginate(blocks, blockHeights, availableForBlocks, blockSpacingPx)

        // Phase 4: render the full pager — pagerState lives inside this subcomposition
        val contentPlaceable = subcompose("pager") {
            PagerContent(
                pages          = pages,
                tone           = tone,
                artist         = artist,
                scrollMode     = scrollMode,
                onToggleScroll = onToggleScroll,
            )
        }.first().measure(constraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceable.place(0, 0)
        }
    }
}

@Composable
private fun PagerContent(
    pages: List<List<ChordBlock>>,
    tone: String,
    artist: String,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        ChordBand(artist = artist, tone = tone, scrollMode = scrollMode, onToggleScroll = onToggleScroll)

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
                ChordPageContent(blocks = pages[index])
            }
        }

        PageProgress(currentPage = pagerState.currentPage, pageCount = pages.size)
    }
}

@Composable
private fun ChordVerticalContent(
    blocks: List<ChordBlock>,
    tone: String,
    artist: String,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ChordBand(artist = artist, tone = tone, scrollMode = scrollMode, onToggleScroll = onToggleScroll)

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
                        ChordBlockView(block)
                    }
                }
            }
        }
    }
}

/** The thin band: artist, "Tom D" and the pages/scroll switch. */
@Composable
private fun ChordBand(artist: String, tone: String, scrollMode: SongScrollMode, onToggleScroll: () -> Unit) {
    SongBand(artist = artist, scrollMode = scrollMode, onToggleScroll = onToggleScroll) {
        if (tone.isNotBlank()) ToneTag(tone)
    }
}

@Composable
private fun ChordPageContent(blocks: List<ChordBlock>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        // Long press selects the text to copy; taps still turn the page.
        SelectionContainer {
            Column {
                blocks.forEachIndexed { index, block ->
                    if (index > 0) Spacer(modifier = Modifier.height(16.dp))
                    ChordBlockView(block)
                }
            }
        }
    }
}

@Composable
private fun EditContent(
    editContent: String,
    isSaving: Boolean,
    saveError: String?,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        if (isSaving) {
            Box(
                modifier         = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
        if (saveError != null) {
            Text(
                text     = saveError,
                color    = MaterialTheme.colorScheme.error,
                style    = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        OutlinedTextField(
            value         = editContent,
            onValueChange = onContentChange,
            modifier      = Modifier
                .fillMaxSize()
                .weight(1f),
            enabled       = !isSaving,
            textStyle     = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            ),
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
