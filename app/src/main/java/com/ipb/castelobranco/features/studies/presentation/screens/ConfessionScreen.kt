package com.ipb.castelobranco.features.studies.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.studies.domain.model.ConfessionChapter
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSearchResult
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineErrorView
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineLoadingView
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineReaderActions
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineSearchField
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineSearchResults
import com.ipb.castelobranco.features.studies.presentation.viewmodel.ConfessionUiState
import com.ipb.castelobranco.features.studies.presentation.viewmodel.ConfessionViewModel

data class ConfessionActions(
    val onBackClick: () -> Unit,
    val onChapterClick: (chapter: Int) -> Unit,
    val onResultClick: (DoctrineSearchResult.Paragraph) -> Unit,
    val onRetry: () -> Unit,
    val onOpenSearch: () -> Unit,
    val onCloseSearch: () -> Unit,
    val onQueryChange: (String) -> Unit,
)

@Composable
fun ConfessionScreen(
    viewModel: ConfessionViewModel,
    onBackClick: () -> Unit,
    onOpenChapter: (chapter: Int, paragraph: Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConfessionContent(
        state = state,
        actions = ConfessionActions(
            onBackClick = onBackClick,
            onChapterClick = { chapter -> onOpenChapter(chapter, 0) },
            onResultClick = { result ->
                viewModel.closeSearch()
                onOpenChapter(result.chapter, result.paragraph)
            },
            onRetry = viewModel::retry,
            onOpenSearch = viewModel::openSearch,
            onCloseSearch = viewModel::closeSearch,
            onQueryChange = viewModel::onQueryChange,
        ),
    )
}

@Composable
fun ConfessionContent(
    state: ConfessionUiState,
    actions: ConfessionActions,
) {
    BaseScreen(
        tabName = "Confissão de Fé",
        showBackArrow = true,
        onBackClick = actions.onBackClick,
        extraActions = {
            if (state.chapters.isNotEmpty()) {
                DoctrineReaderActions(onSearchClick = actions.onOpenSearch, onFontSizeClick = null)
            }
        },
        topBarExtension = {
            if (state.isSearching) {
                DoctrineSearchField(
                    query = state.query,
                    placeholder = "Buscar na Confissão",
                    onQueryChange = actions.onQueryChange,
                    onClose = actions.onCloseSearch,
                )
            }
        },
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        val results = state.results
        when {
            state.chapters.isNotEmpty() && state.isSearching && results != null -> DoctrineSearchResults(
                results = results,
                onResultClick = { (it as? DoctrineSearchResult.Paragraph)?.let(actions.onResultClick) },
                modifier = modifier,
            )
            state.chapters.isNotEmpty() -> ChapterList(
                title = state.title,
                chapters = state.chapters,
                onChapterClick = actions.onChapterClick,
                modifier = modifier,
            )
            state.isLoading -> DoctrineLoadingView(modifier)
            state.error != null -> DoctrineErrorView(message = state.error, onRetry = actions.onRetry, modifier = modifier)
            else -> DoctrineLoadingView(modifier)
        }
    }
}

@Composable
private fun ChapterList(
    title: String,
    chapters: List<ConfessionChapter>,
    onChapterClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${chapters.size} capítulos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        items(chapters, key = { it.number }) { chapter ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChapterClick(chapter.number) }
                    .defaultMinSize(minHeight = 52.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = chapter.number.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(text = chapter.title, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfessionContentPreview() {
    MaterialTheme {
        ConfessionContent(
            state = ConfessionUiState(
                title = "A Confissão de Fé de Westminster",
                chapters = listOf(
                    ConfessionChapter(1, "Da Sagradas Escrituras", emptyList()),
                    ConfessionChapter(29, "Da ceia do Senhor", emptyList()),
                ),
                isLoading = false,
            ),
            actions = ConfessionActions({}, {}, {}, {}, {}, {}, {}),
        )
    }
}
