package com.ipb.castelobranco.features.studies.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.studies.domain.model.DoctrineKind
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSummary
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineErrorView
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineLoadingView
import com.ipb.castelobranco.features.studies.presentation.components.StudiesEntryCard
import com.ipb.castelobranco.features.studies.presentation.viewmodel.DoctrineListUiState
import com.ipb.castelobranco.features.studies.presentation.viewmodel.DoctrineListViewModel

@Composable
fun DoctrineListScreen(
    viewModel: DoctrineListViewModel,
    onBackClick: () -> Unit,
    onOpenDocument: (DoctrineSummary) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BaseScreen(tabName = "Doutrina", showBackArrow = true, onBackClick = onBackClick) { innerPadding ->
        DoctrineListContent(
            state = state,
            onOpenDocument = onOpenDocument,
            onRetry = viewModel::retry,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun DoctrineListContent(
    state: DoctrineListUiState,
    onOpenDocument: (DoctrineSummary) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.documents.isNotEmpty() -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "DOCUMENTOS DE WESTMINSTER",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.documents, key = { it.slug }) { document ->
                StudiesEntryCard(
                    title = document.shortTitle,
                    subtitle = document.itemLabel,
                    badge = document.itemCount.toString(),
                    onClick = { onOpenDocument(document) },
                )
            }
            item {
                Text(
                    text = "Toque numa referência bíblica para abrir o texto na Bíblia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.isLoading -> DoctrineLoadingView(modifier)
        state.error != null -> DoctrineErrorView(message = state.error, onRetry = onRetry, modifier = modifier)
        else -> DoctrineLoadingView(modifier)
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctrineListContentPreview() {
    MaterialTheme {
        DoctrineListContent(
            state = DoctrineListUiState(
                documents = listOf(
                    DoctrineSummary(
                        "westminster-shorter-catechism", "Breve Catecismo de Westminster",
                        "Breve Catecismo", DoctrineKind.CATECHISM, 107,
                    ),
                    DoctrineSummary(
                        "westminster-confession-of-faith", "A Confissão de Fé de Westminster",
                        "Confissão de Fé", DoctrineKind.CONFESSION, 33,
                    ),
                ),
                isLoading = false,
            ),
            onOpenDocument = {},
            onRetry = {},
        )
    }
}
