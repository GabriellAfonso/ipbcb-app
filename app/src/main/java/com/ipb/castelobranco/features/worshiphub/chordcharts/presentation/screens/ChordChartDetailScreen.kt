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
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordBlock
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordLine
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.LineToken
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.state.ChordChartDetailUiState
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.viewmodel.ChordChartDetailViewModel

private val ChordColor = BrandColors.Orange
private val SectionTitleColor = BrandColors.Green

@Composable
fun ChordChartDetailScreen(
    viewModel: ChordChartDetailViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollMode by viewModel.scrollMode.collectAsStateWithLifecycle()
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
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
    onBackClick: () -> Unit,
    onEnterEdit: () -> Unit,
    onEditContentChange: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
) {
    BaseScreen(
        tabName       = state.songName.ifEmpty { "Cifra" },
        logoRes       = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick   = onBackClick,
        extraActions  = {
            if (state.canEdit) {
                EditOverflowMenu(
                    isEditing  = state.isEditing,
                    isSaving   = state.isSaving,
                    onEdit     = onEnterEdit,
                    onSave     = onSaveEdit,
                    onCancel   = onCancelEdit,
                )
            }
        },
    ) { innerPadding ->
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
            scrollMode == SongScrollMode.VERTICAL -> ChordVerticalContent(
                blocks         = state.blocks,
                tone           = state.tone,
                scrollMode     = scrollMode,
                onToggleScroll = onToggleScroll,
                modifier       = Modifier.padding(innerPadding),
            )
            else -> ChordPager(
                blocks         = state.blocks,
                tone           = state.tone,
                scrollMode     = scrollMode,
                onToggleScroll = onToggleScroll,
                modifier       = Modifier.padding(innerPadding),
            )
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
        // Column wrapper is required — SectionBlock emits multiple root nodes (Text, Spacers, Rows)
        // with no container, so .firstOrNull() without it would only measure the first child.
        val blockHeights = blocks.mapIndexed { i, block ->
            subcompose("m_$i") {
                Column {
                    if (block.isIntro) IntroBlock(block) else SectionBlock(block)
                }
            }.firstOrNull()
                ?.measure(Constraints(maxWidth = measureWidth))
                ?.height ?: 0
        }

        // Phase 2: measure chrome (header + dots) to determine pager area height
        val headerHeight = subcompose("chrome_header") {
            PagerHeader(
                tone           = tone,
                pageInfo       = "1 / 1",
                scrollMode     = scrollMode,
                onToggleScroll = {},
            )
        }.first().measure(Constraints(maxWidth = constraints.maxWidth)).height

        val dotsHeight = subcompose("chrome_dots") {
            PageDotIndicator(
                pageCount   = 1,
                currentPage = 0,
                modifier    = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
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
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        PagerHeader(
            tone           = tone,
            pageInfo       = "${pagerState.currentPage + 1} / ${pages.size}",
            scrollMode     = scrollMode,
            onToggleScroll = onToggleScroll,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .tapToPaginate(pagerState, scope),
        ) {
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                ChordPageContent(blocks = pages[index])
            }
        }

        PageDotIndicator(
            pageCount   = pages.size,
            currentPage = pagerState.currentPage,
            modifier    = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun ChordVerticalContent(
    blocks: List<ChordBlock>,
    tone: String,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PagerHeader(
            tone           = tone,
            pageInfo       = null,
            scrollMode     = scrollMode,
            onToggleScroll = onToggleScroll,
        )

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
                        if (block.isIntro) IntroBlock(block) else SectionBlock(block)
                    }
                }
            }
        }
    }
}

@Composable
private fun PagerHeader(
    tone: String,
    pageInfo: String?,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text       = "Key: $tone",
            style      = MaterialTheme.typography.bodyMedium,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (pageInfo != null) {
                Text(
                    text  = pageInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            ScrollModeToggle(scrollMode = scrollMode, onToggle = onToggleScroll)
        }
    }
}

@Composable
private fun ScrollModeToggle(scrollMode: SongScrollMode, onToggle: () -> Unit) {
    val (icon, description) = when (scrollMode) {
        SongScrollMode.HORIZONTAL -> Icons.AutoMirrored.Filled.ViewList to "Modo vertical"
        SongScrollMode.VERTICAL   -> Icons.Filled.ViewColumn to "Modo horizontal"
    }
    IconButton(onClick = onToggle) {
        Icon(
            imageVector        = icon,
            contentDescription = description,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
                    if (block.isIntro) IntroBlock(block) else SectionBlock(block)
                }
            }
        }
    }
}

@Composable
private fun IntroBlock(block: ChordBlock) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text       = "Intro:",
            style      = MaterialTheme.typography.titleMedium,
            color      = SectionTitleColor,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(8.dp))
        block.lines.forEach { line ->
            line.tokens.filterIsInstance<LineToken.Chord>().forEach { chord ->
                Text(
                    text       = chord.value,
                    color      = ChordColor,
                    fontWeight = FontWeight.Bold,
                    style      = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun SectionBlock(block: ChordBlock) {
    if (block.title != null) {
        Text(
            text       = block.title,
            style      = MaterialTheme.typography.titleMedium,
            color      = SectionTitleColor,
            fontWeight = FontWeight.Bold,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    block.lines.forEach { line ->
        ChordLineRow(line = line)
        Spacer(modifier = Modifier.height(2.dp))
    }
}

private data class ChordLyricGroup(val chord: String?, val charOffset: Int = 0, val lyrics: String)

private fun groupTokens(tokens: List<LineToken>): List<ChordLyricGroup> {
    val groups = mutableListOf<ChordLyricGroup>()
    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        when (token) {
            is LineToken.Chord -> {
                val lyrics = (tokens.getOrNull(i + 1) as? LineToken.Lyrics)?.value ?: ""
                groups += ChordLyricGroup(token.value, token.charOffset, lyrics)
                i += 2
            }
            is LineToken.Lyrics -> {
                groups += ChordLyricGroup(null, 0, token.value)
                i += 1
            }
        }
    }
    return groups
}

@Composable
private fun ChordLineRow(line: ChordLine) {
    val lyricColor = MaterialTheme.colorScheme.onSurface
    val groups = groupTokens(line.tokens)
    FlowRow {
        groups.forEach { group ->
            ChordLyricGroupItem(group = group, lyricColor = lyricColor)
        }
    }
}

@Composable
private fun ChordLyricGroupItem(group: ChordLyricGroup, lyricColor: Color) {
    var chordOffsetXPx by remember(group.chord, group.charOffset, group.lyrics) {
        mutableFloatStateOf(0f)
    }

    val chordContent: @Composable () -> Unit = {
        Text(
            text       = group.chord ?: "",
            color      = ChordColor,
            fontWeight = FontWeight.Bold,
            style      = MaterialTheme.typography.labelMedium,
            softWrap   = false,
            maxLines   = 1,
        )
    }

    val lyricsContent: @Composable () -> Unit = {
        Text(
            text         = group.lyrics,
            color        = lyricColor,
            style        = MaterialTheme.typography.bodyLarge,
            softWrap     = false,
            maxLines     = 1,
            onTextLayout = { result ->
                if (group.chord != null && group.lyrics.isNotEmpty()) {
                    val idx = group.charOffset.coerceIn(0, group.lyrics.length - 1)
                    chordOffsetXPx = result.getCursorRect(idx).left
                }
            },
        )
    }

    Layout(contents = listOf(chordContent, lyricsContent)) { measurables, _ ->
        val chordPlaceable  = measurables[0].firstOrNull()?.measure(Constraints())
        val lyricsPlaceable = measurables[1].firstOrNull()?.measure(Constraints())
            ?: return@Layout layout(0, 0) {}

        val chordH    = chordPlaceable?.height ?: 0
        val chordLeft = if (group.chord != null) chordOffsetXPx.roundToInt() else 0
        val chordEnd  = chordLeft + (chordPlaceable?.width ?: 0)

        layout(
            width  = maxOf(lyricsPlaceable.width, chordEnd),
            height = chordH + lyricsPlaceable.height,
        ) {
            chordPlaceable?.placeRelative(chordLeft, 0)
            lyricsPlaceable.placeRelative(0, chordH)
        }
    }
}

@Composable
private fun PageDotIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Surface(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) 8.dp else 6.dp),
                shape    = CircleShape,
                color    = if (isSelected) ChordColor
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            ) {}
        }
    }
}

@Composable
private fun EditOverflowMenu(
    isEditing: Boolean,
    isSaving: Boolean,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector        = Icons.Default.MoreVert,
                contentDescription = "Menu",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (isEditing) {
                DropdownMenuItem(
                    text    = { Text("Salvar") },
                    onClick = { expanded = false; onSave() },
                    enabled = !isSaving,
                )
                DropdownMenuItem(
                    text    = { Text("Cancelar") },
                    onClick = { expanded = false; onCancel() },
                    enabled = !isSaving,
                )
            } else {
                DropdownMenuItem(
                    text    = { Text("Editar") },
                    onClick = { expanded = false; onEdit() },
                )
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
