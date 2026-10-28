package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.ipb.castelobranco.core.presentation.components.PanelAction
import com.ipb.castelobranco.core.presentation.components.PanelHeader
import com.ipb.castelobranco.core.presentation.components.PanelMoreButton
import com.ipb.castelobranco.core.presentation.components.TopActionPanel
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.EditBarActions
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.LyricsTextSize
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.PageProgress
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongBand
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.TextSizeButtons

private const val EDIT_LABEL = "Editar letra"
private const val EDIT_HINT = "Muda o texto"

/** A tap on the right half turns the page, on the left half goes back. */
private const val HALF_SCREEN = 0.5f
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
    var isPanelOpen by rememberSaveable { mutableStateOf(false) }
    // Only while the lyrics stay open (`specs/worshiphub/spec.md` §5.3).
    var textSize by rememberSaveable { mutableIntStateOf(LyricsTextSize.DEFAULT) }
    val size = TextSizeControl(
        sp = textSize,
        onShrink = { textSize = (textSize - LyricsTextSize.STEP).coerceAtLeast(LyricsTextSize.MIN) },
        onGrow = { textSize = (textSize + LyricsTextSize.STEP).coerceAtMost(LyricsTextSize.MAX) },
    )
    BaseScreen(
        tabName       = state.songName.ifEmpty { "Letra" },
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
            state.stanzas.isEmpty() -> ErrorState("Sem conteúdo disponível", Modifier.padding(innerPadding))
            // Opens straight in the saved mode: nothing is drawn until it is known.
            scrollMode == null -> LoadingState(Modifier.padding(innerPadding))
            scrollMode == SongScrollMode.VERTICAL -> LyricsVerticalContent(
                stanzas        = state.stanzas,
                artist         = state.artist,
                size           = size,
                scrollMode     = scrollMode,
                onToggleScroll = onToggleScroll,
                modifier       = Modifier.padding(innerPadding),
            )
            else -> LyricsPager(
                stanzas        = state.stanzas,
                artist         = state.artist,
                size           = size,
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
                header = PanelHeader(name = state.songName, detail = state.artist.takeIf { it.isNotBlank() }),
            )
        }
        }
    }
}

/** The lyrics' text size and the band's "A−" / "A+". */
private data class TextSizeControl(val sp: Int, val onShrink: () -> Unit, val onGrow: () -> Unit) {
    val canShrink: Boolean get() = sp > LyricsTextSize.MIN
    val canGrow: Boolean get() = sp < LyricsTextSize.MAX
}

/**
 * Measures each stanza's height, computes page splits, then subcomposes the full
 * pager UI with the pages already known — same pattern as ChordChartDetailScreen.
 */
@Composable
private fun LyricsPager(
    stanzas: List<LyricsStanza>,
    artist: String,
    size: TextSizeControl,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier = modifier.fillMaxSize()) { constraints ->
        val stanzaSpacingPx = 16.dp.roundToPx()
        val hPaddingPx      = 40.dp.roundToPx()  // matches LyricsPageContent padding(horizontal = 20.dp)
        val vPaddingPx      = 24.dp.roundToPx()  // matches LyricsPageContent padding(vertical = 12.dp)
        val measureWidth    = (constraints.maxWidth - hPaddingPx).coerceAtLeast(0)

        // Phase 1: measure each stanza at the correct render width
        val stanzaHeights = stanzas.mapIndexed { i, stanza ->
            subcompose("m_$i") {
                Column { StanzaBlock(stanza, size.sp) }
            }.firstOrNull()
                ?.measure(Constraints(maxWidth = measureWidth))
                ?.height ?: 0
        }

        // Phase 2: measure chrome (header + dots) for available height calculation
        val headerHeight = subcompose("chrome_header") {
            LyricsBand(artist = artist, size = size, scrollMode = scrollMode, onToggleScroll = {})
        }.first().measure(Constraints(maxWidth = constraints.maxWidth)).height

        val dotsHeight = subcompose("chrome_dots") {
            PageProgress(currentPage = 0, pageCount = 1)
        }.first().measure(Constraints(maxWidth = constraints.maxWidth)).height

        // Phase 3: compute pages
        val availableForStanzas = (constraints.maxHeight - headerHeight - dotsHeight - vPaddingPx)
            .coerceAtLeast(1)
        val pages = paginate(stanzas, stanzaHeights, availableForStanzas, stanzaSpacingPx)

        // Phase 4: render full pager — pagerState lives inside this subcomposition
        val contentPlaceable = subcompose("pager") {
            PagerContent(
                pages          = pages,
                artist         = artist,
                size           = size,
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

@Composable
private fun PagerContent(
    pages: List<List<LyricsStanza>>,
    artist: String,
    size: TextSizeControl,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope      = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        LyricsBand(artist = artist, size = size, scrollMode = scrollMode, onToggleScroll = onToggleScroll)

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
                LyricsPageContent(stanzas = pages[index], textSize = size.sp)
            }
        }

        PageProgress(currentPage = pagerState.currentPage, pageCount = pages.size)
    }
}

@Composable
private fun LyricsVerticalContent(
    stanzas: List<LyricsStanza>,
    artist: String,
    size: TextSizeControl,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LyricsBand(artist = artist, size = size, scrollMode = scrollMode, onToggleScroll = onToggleScroll)

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
                        StanzaBlock(stanza, size.sp)
                    }
                }
            }
        }
    }
}

/** The thin band: artist, "A−" / "A+" and the pages/scroll switch. */
@Composable
private fun LyricsBand(artist: String, size: TextSizeControl, scrollMode: SongScrollMode, onToggleScroll: () -> Unit) {
    SongBand(artist = artist, scrollMode = scrollMode, onToggleScroll = onToggleScroll) {
        TextSizeButtons(size.canShrink, size.canGrow, size.onShrink, size.onGrow)
    }
}

@Composable
private fun LyricsPageContent(stanzas: List<LyricsStanza>, textSize: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        // Long press selects the text to copy; taps still turn the page.
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

@Composable
private fun StanzaBlock(stanza: LyricsStanza, textSize: Int) {
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
