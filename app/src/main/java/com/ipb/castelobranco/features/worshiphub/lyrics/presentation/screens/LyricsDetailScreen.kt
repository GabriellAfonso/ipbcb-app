package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.screens

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.data.local.SongScrollMode
import com.ipb.castelobranco.core.domain.model.TextScale
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.modifier.tapToPaginate
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.parser.LyricsStanza
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.state.LyricsDetailUiState
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.viewmodel.LyricsDetailViewModel
import com.ipb.castelobranco.core.presentation.components.KeepScreenOn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.core.presentation.components.isCompactHeight
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.CancelBarAction
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongFormScaffold
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongFormSaveAction
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongSummary
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.components.LyricsEditor
import androidx.compose.foundation.layout.PaddingValues
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.LyricsTextSize
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.PageProgress
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongBand
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.TextSizeRow

private const val EDIT_LABEL = "Editar letra"
private const val SAVE_LABEL = "Salvar letra"

/** A tap on the right half turns the page, on the left half goes back. */
private const val HALF_SCREEN = 0.5f
private const val LANDSCAPE_PAGE_COLUMNS = 2
private val PageColumnGap = 28.dp
private const val LINE_HEIGHT = 1.3f

@Composable
fun LyricsDetailScreen(
    viewModel: LyricsDetailViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollMode by viewModel.scrollMode.collectAsStateWithLifecycle()
    KeepScreenOn()
    LyricsDetailContent(
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
private fun LyricsDetailContent(
    state: LyricsDetailUiState,
    scrollMode: SongScrollMode?,
    onToggleScroll: () -> Unit,
    onBackClick: () -> Unit,
    onEnterEdit: () -> Unit,
    onEditContentChange: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
) {
    // Only while the lyrics stay open (`specs/worshiphub/spec.md` §5.3).
    var textPercent by rememberSaveable { mutableIntStateOf(TextScale.DEFAULT) }
    val textSize = LyricsTextSize.sp(textPercent)
    BaseScreen(
        tabName       = if (state.isEditing) EDIT_LABEL else state.songName.ifEmpty { "Letra" },
        logoRes       = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick   = onBackClick,
        extraActions  = {
            when {
                state.isEditing -> {
                    SongFormSaveAction(SAVE_LABEL, state.editMissing, state.isSaving, onSaveEdit)
                    CancelBarAction(state.isSaving, onCancelEdit)
                }
                else -> OverflowMenu(
                    actions  = listOfNotNull(
                        MenuAction(Icons.Filled.Edit, EDIT_LABEL, onEnterEdit).takeIf { state.canEdit },
                    ),
                    controls = if (state.stanzas.isNotEmpty()) {
                        { TextSizeRow(percent = textPercent, onPercentChange = { textPercent = it }) }
                    } else {
                        null
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingState(Modifier.padding(innerPadding))
            state.error != null -> ErrorState(state.error, Modifier.padding(innerPadding))
            state.isEditing -> EditContent(state, innerPadding, onEditContentChange, onSaveEdit)
            state.stanzas.isEmpty() -> ErrorState("Sem conteúdo disponível", Modifier.padding(innerPadding))
            // Opens straight in the saved mode: nothing is drawn until it is known.
            scrollMode == null -> LoadingState(Modifier.padding(innerPadding))
            scrollMode == SongScrollMode.VERTICAL -> LyricsVerticalContent(
                stanzas        = state.stanzas,
                artist         = state.artist,
                textSize       = textSize,
                scrollMode     = scrollMode,
                onToggleScroll = onToggleScroll,
                modifier       = Modifier.padding(innerPadding),
            )
            else -> LyricsPager(
                stanzas        = state.stanzas,
                artist         = state.artist,
                textSize       = textSize,
                scrollMode     = scrollMode,
                onToggleScroll = onToggleScroll,
                modifier       = Modifier.padding(innerPadding),
            )
        }
        }
    }
}

/**
 * Measures each stanza's height, computes page splits, then subcomposes the full
 * pager UI with the pages already known — same pattern as ChordChartDetailScreen.
 */
@Composable
private fun LyricsPager(
    stanzas: List<LyricsStanza>,
    artist: String,
    textSize: Float,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Lying down each page shows its stanzas in columns, side by side (`specs/worshiphub/spec.md` §5).
    val columns = if (isCompactHeight()) LANDSCAPE_PAGE_COLUMNS else 1
    SubcomposeLayout(modifier = modifier.fillMaxSize()) { constraints ->
        val stanzaSpacingPx = 16.dp.roundToPx()
        val hPaddingPx      = 40.dp.roundToPx()  // matches LyricsPageContent padding(horizontal = 20.dp)
        val vPaddingPx      = 24.dp.roundToPx()  // matches LyricsPageContent padding(vertical = 12.dp)
        val gapsPx          = PageColumnGap.roundToPx() * (columns - 1)
        val measureWidth    = ((constraints.maxWidth - hPaddingPx - gapsPx) / columns).coerceAtLeast(0)

        // Phase 1: measure each stanza at the correct render width
        val stanzaHeights = stanzas.mapIndexed { i, stanza ->
            subcompose("m_$i") {
                Column { StanzaBlock(stanza, textSize) }
            }.firstOrNull()
                ?.measure(Constraints(maxWidth = measureWidth))
                ?.height ?: 0
        }

        // Phase 2: measure chrome (header + dots) for available height calculation
        val headerHeight = subcompose("chrome_header") {
            LyricsBand(artist = artist, scrollMode = scrollMode, onToggleScroll = {})
        }.first().measure(Constraints(maxWidth = constraints.maxWidth)).height

        val dotsHeight = subcompose("chrome_dots") {
            PageProgress(currentPage = 0, pageCount = 1)
        }.first().measure(Constraints(maxWidth = constraints.maxWidth)).height

        // Phase 3: compute pages
        val availableForStanzas = (constraints.maxHeight - headerHeight - dotsHeight - vPaddingPx)
            .coerceAtLeast(1)
        val pages = paginate(stanzas, stanzaHeights, availableForStanzas, stanzaSpacingPx).chunked(columns)

        // Phase 4: render full pager — pagerState lives inside this subcomposition
        val contentPlaceable = subcompose("pager") {
            PagerContent(
                pages          = pages,
                artist         = artist,
                textSize       = textSize,
                scrollMode     = scrollMode,
                onToggleScroll = onToggleScroll,
            )
        }.first().measure(constraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceable.place(0, 0)
        }
    }
}

private fun paginate(
    stanzas: List<LyricsStanza>,
    heights: List<Int>,
    availableHeight: Int,
    spacingPx: Int,
): List<List<LyricsStanza>> {
    if (stanzas.isEmpty()) return emptyList()

    val pages = mutableListOf<MutableList<LyricsStanza>>()
    var currentPage = mutableListOf<LyricsStanza>()
    var usedHeight = 0

    stanzas.forEachIndexed { i, stanza ->
        val h       = heights[i]
        val spacing = if (currentPage.isEmpty()) 0 else spacingPx
        if (currentPage.isNotEmpty() && usedHeight + spacing + h > availableHeight) {
            pages += currentPage
            currentPage = mutableListOf()
            usedHeight = 0
        }
        currentPage += stanza
        usedHeight += (if (usedHeight == 0) 0 else spacingPx) + h
    }

    if (currentPage.isNotEmpty()) pages += currentPage
    return pages
}

/** [pages]: each page's columns, each column's stanzas. */
@Composable
private fun PagerContent(
    pages: List<List<List<LyricsStanza>>>,
    artist: String,
    textSize: Float,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope      = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        LyricsBand(artist = artist, scrollMode = scrollMode, onToggleScroll = onToggleScroll)

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
                LyricsPageContent(columns = pages[index], textSize = textSize)
            }
        }

        PageProgress(currentPage = pagerState.currentPage, pageCount = pages.size)
    }
}

@Composable
private fun LyricsVerticalContent(
    stanzas: List<LyricsStanza>,
    artist: String,
    textSize: Float,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LyricsBand(artist = artist, scrollMode = scrollMode, onToggleScroll = onToggleScroll)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            SelectionContainer {
                Column {
                    stanzas.forEachIndexed { index, stanza ->
                        if (index > 0) Spacer(modifier = Modifier.height(16.dp))
                        StanzaBlock(stanza, textSize)
                    }
                }
            }
        }
    }
}

/** The thin band: the artist and the pages/scroll switch. */
@Composable
private fun LyricsBand(artist: String, scrollMode: SongScrollMode, onToggleScroll: () -> Unit) {
    SongBand(artist = artist, scrollMode = scrollMode, onToggleScroll = onToggleScroll) {}
}

@Composable
private fun LyricsPageContent(columns: List<List<LyricsStanza>>, textSize: Float) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(PageColumnGap),
    ) {
        columns.forEach { stanzas ->
            // Long press selects the text to copy; taps still turn the page.
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Column {
                    stanzas.forEachIndexed { index, stanza ->
                        if (index > 0) Spacer(modifier = Modifier.height(16.dp))
                        StanzaBlock(stanza, textSize)
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

@Composable
private fun StanzaBlock(stanza: LyricsStanza, textSize: Float) {
    val style = MaterialTheme.typography.titleLarge
        .copy(fontSize = textSize.sp, lineHeight = (textSize * LINE_HEIGHT).sp)
    Column {
        stanza.lines.forEach { line ->
            Text(
                text  = line,
                style = style,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

/** The edit form: the new lyrics' text, with the song read-only (`specs/worshiphub/spec.md` §5.3). */
@Composable
private fun EditContent(
    state: LyricsDetailUiState,
    innerPadding: PaddingValues,
    onContentChange: (String) -> Unit,
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
            LyricsEditor(
                content         = state.editContent,
                previewStanzas  = state.editPreviewStanzas,
                enabled         = !state.isSaving,
                onContentChange = onContentChange,
            )
        },
    ) {
        SongSummary(title = state.songName, detail = state.artist)
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
