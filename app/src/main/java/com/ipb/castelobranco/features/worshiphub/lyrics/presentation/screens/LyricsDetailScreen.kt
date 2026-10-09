package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.data.local.SongScrollMode
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.modifier.tapToPaginate
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.parser.LyricsStanza
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.state.LyricsDetailUiState
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.viewmodel.LyricsDetailViewModel

private val DotColor   = BrandColors.Blue
private val TitleColor = BrandColors.Orange

@Composable
fun LyricsDetailScreen(
    viewModel: LyricsDetailViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollMode by viewModel.scrollMode.collectAsStateWithLifecycle()
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
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
    onBackClick: () -> Unit,
    onEnterEdit: () -> Unit,
    onEditContentChange: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
) {
    BaseScreen(
        tabName       = "Letra",
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
            state.stanzas.isEmpty() -> ErrorState("Sem conteúdo disponível", Modifier.padding(innerPadding))
            scrollMode == SongScrollMode.VERTICAL -> LyricsVerticalContent(
                stanzas        = state.stanzas,
                songName       = state.songName,
                scrollMode     = scrollMode,
                onToggleScroll = onToggleScroll,
                modifier       = Modifier.padding(innerPadding),
            )
            else -> LyricsPager(
                stanzas        = state.stanzas,
                songName       = state.songName,
                scrollMode     = scrollMode,
                onToggleScroll = onToggleScroll,
                modifier       = Modifier.padding(innerPadding),
            )
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
    songName: String,
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
                Column { StanzaBlock(stanza) }
            }.firstOrNull()
                ?.measure(Constraints(maxWidth = measureWidth))
                ?.height ?: 0
        }

        // Phase 2: measure chrome (header + dots) for available height calculation
        val headerHeight = subcompose("chrome_header") {
            LyricsHeader(songName = songName, scrollMode = scrollMode, onToggleScroll = {})
        }.first().measure(Constraints(maxWidth = constraints.maxWidth)).height

        val dotsHeight = subcompose("chrome_dots") {
            PageDotIndicator(
                pageCount   = 1,
                currentPage = 0,
                modifier    = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
        }.first().measure(Constraints(maxWidth = constraints.maxWidth)).height

        // Phase 3: compute pages
        val availableForStanzas = (constraints.maxHeight - headerHeight - dotsHeight - vPaddingPx)
            .coerceAtLeast(1)
        val pages = paginate(stanzas, stanzaHeights, availableForStanzas, stanzaSpacingPx)

        // Phase 4: render full pager — pagerState lives inside this subcomposition
        val contentPlaceable = subcompose("pager") {
            PagerContent(
                pages          = pages,
                songName       = songName,
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
    songName: String,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope      = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        LyricsHeader(songName = songName, scrollMode = scrollMode, onToggleScroll = onToggleScroll)

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
                LyricsPageContent(stanzas = pages[index])
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
private fun LyricsVerticalContent(
    stanzas: List<LyricsStanza>,
    songName: String,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LyricsHeader(songName = songName, scrollMode = scrollMode, onToggleScroll = onToggleScroll)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            stanzas.forEachIndexed { index, stanza ->
                if (index > 0) Spacer(modifier = Modifier.height(16.dp))
                StanzaBlock(stanza)
            }
        }
    }
}

@Composable
private fun LyricsHeader(
    songName: String,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = songName,
            color    = TitleColor,
            style    = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        ScrollModeToggle(scrollMode = scrollMode, onToggle = onToggleScroll)
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
private fun LyricsPageContent(stanzas: List<LyricsStanza>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        stanzas.forEachIndexed { index, stanza ->
            if (index > 0) Spacer(modifier = Modifier.height(16.dp))
            StanzaBlock(stanza)
        }
    }
}

@Composable
private fun StanzaBlock(stanza: LyricsStanza) {
    Column {
        stanza.lines.forEach { line ->
            Text(
                text  = line,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
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
                color    = if (isSelected) DotColor
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
