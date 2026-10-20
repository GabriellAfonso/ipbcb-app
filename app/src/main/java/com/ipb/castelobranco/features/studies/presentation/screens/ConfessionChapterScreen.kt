package com.ipb.castelobranco.features.studies.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.studies.domain.model.ConfessionChapter
import com.ipb.castelobranco.features.studies.domain.model.ConfessionParagraph
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSearchResult
import com.ipb.castelobranco.features.studies.domain.model.ParagraphList
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineErrorView
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineFontSizeBar
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineLoadingView
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineReaderActions
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineSearchField
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineSearchResults
import com.ipb.castelobranco.features.studies.presentation.viewmodel.ConfessionChapterUiState
import com.ipb.castelobranco.features.studies.presentation.viewmodel.ConfessionChapterViewModel

data class ConfessionChapterActions(
    val onBackClick: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onRetry: () -> Unit,
    val onOpenSearch: () -> Unit,
    val onCloseSearch: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onResultClick: (DoctrineSearchResult) -> Unit,
    val onIncreaseFont: () -> Unit,
    val onDecreaseFont: () -> Unit,
)

@Composable
fun ConfessionChapterScreen(
    viewModel: ConfessionChapterViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel) {
        viewModel.scroll.collect { request ->
            // +1 = chapter header item
            runCatching { listState.scrollToItem(if (request.index == 0) 0 else request.index + 1) }
        }
    }

    ConfessionChapterContent(
        state = state,
        listState = listState,
        actions = ConfessionChapterActions(
            onBackClick = onBackClick,
            onPrevious = viewModel::previous,
            onNext = viewModel::next,
            onRetry = viewModel::retry,
            onOpenSearch = viewModel::openSearch,
            onCloseSearch = viewModel::closeSearch,
            onQueryChange = viewModel::onQueryChange,
            onResultClick = viewModel::onResultClick,
            onIncreaseFont = viewModel::increaseFont,
            onDecreaseFont = viewModel::decreaseFont,
        ),
    )
}

@Composable
fun ConfessionChapterContent(
    state: ConfessionChapterUiState,
    listState: LazyListState,
    actions: ConfessionChapterActions,
) {
    var showFontBar by rememberSaveable { mutableStateOf(false) }
    val chapter = state.chapter

    BaseScreen(
        tabName = "Confissão de Fé",
        showBackArrow = true,
        onBackClick = actions.onBackClick,
        extraActions = {
            if (chapter != null) {
                DoctrineReaderActions(
                    onSearchClick = actions.onOpenSearch,
                    onFontSizeClick = { showFontBar = !showFontBar },
                )
            }
        },
        topBarExtension = {
            when {
                state.isSearching -> DoctrineSearchField(
                    query = state.query,
                    placeholder = "Buscar na Confissão",
                    onQueryChange = actions.onQueryChange,
                    onClose = actions.onCloseSearch,
                )
                showFontBar -> DoctrineFontSizeBar(
                    fontSize = state.fontSize.size,
                    canDecrease = state.fontSize.canDecrease,
                    canIncrease = state.fontSize.canIncrease,
                    onDecrease = actions.onDecreaseFont,
                    onIncrease = actions.onIncreaseFont,
                )
            }
        },
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        val results = state.results
        when {
            chapter != null && state.isSearching && results != null ->
                DoctrineSearchResults(results = results, onResultClick = actions.onResultClick, modifier = modifier)
            chapter != null -> Column(modifier = modifier.fillMaxSize()) {
                ParagraphList(
                    chapter = chapter,
                    fontSize = state.fontSize.size,
                    listState = listState,
                    modifier = Modifier.weight(1f),
                )
                ChapterNavigation(
                    hasPrevious = state.hasPrevious,
                    hasNext = state.hasNext,
                    onPrevious = actions.onPrevious,
                    onNext = actions.onNext,
                )
            }
            state.isLoading -> DoctrineLoadingView(modifier)
            state.error != null -> DoctrineErrorView(message = state.error, onRetry = actions.onRetry, modifier = modifier)
            else -> DoctrineLoadingView(modifier)
        }
    }
}

@Composable
private fun ParagraphList(
    chapter: ConfessionChapter,
    fontSize: Float,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header-${chapter.number}") {
            Column {
                Text(
                    text = "CAPÍTULO ${chapter.number}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        items(chapter.paragraphs, key = { "${chapter.number}.${it.number}" }) { paragraph ->
            ParagraphBlock(paragraph = paragraph, fontSize = fontSize)
        }
    }
}

@Composable
private fun ParagraphBlock(paragraph: ConfessionParagraph, fontSize: Float) {
    val bodyLineHeight = (fontSize * LINE_HEIGHT).sp
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "${paragraph.number}.",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = paragraph.text,
            fontFamily = FontFamily.Serif,
            fontSize = fontSize.sp,
            lineHeight = bodyLineHeight,
        )
        paragraph.lists.forEach { list -> ListBlock(list = list, fontSize = fontSize) }
        paragraph.textAfter?.let { after ->
            Text(text = after, fontFamily = FontFamily.Serif, fontSize = fontSize.sp, lineHeight = bodyLineHeight)
        }
    }
}

@Composable
private fun ListBlock(list: ParagraphList, fontSize: Float) {
    Column(modifier = Modifier.padding(start = 8.dp)) {
        Text(
            text = list.heading,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = list.items.joinToString(separator = " · "),
            fontFamily = FontFamily.Serif,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * LINE_HEIGHT).sp,
        )
    }
}

@Composable
private fun ChapterNavigation(
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onPrevious, enabled = hasPrevious) { Text("Anterior") }
                OutlinedButton(onClick = onNext, enabled = hasNext) { Text("Próximo") }
            }
        }
    }
}

private const val LINE_HEIGHT = 1.55f

@Preview(showBackground = true)
@Composable
private fun ConfessionChapterContentPreview() {
    MaterialTheme {
        ConfessionChapterContent(
            state = ConfessionChapterUiState(
                chapter = ConfessionChapter(
                    number = 1,
                    title = "Da Sagradas Escrituras",
                    paragraphs = listOf(
                        ConfessionParagraph(
                            number = 2,
                            text = "Sob o nome de Escritura Sagrada, ou Palavra de Deus escrita, incluem-se agora:",
                            lists = listOf(ParagraphList("Velho Testamento", listOf("Gênesis", "Êxodo", "Levítico"))),
                            textAfter = "Todos esses livros foram dados por inspiração de Deus.",
                        ),
                    ),
                ),
                hasNext = true,
                isLoading = false,
            ),
            listState = rememberLazyListState(),
            actions = ConfessionChapterActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}
