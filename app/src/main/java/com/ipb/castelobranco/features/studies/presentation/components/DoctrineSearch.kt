package com.ipb.castelobranco.features.studies.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSearchResult

@Composable
fun DoctrineSearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focusRequester),
            singleLine = true,
            placeholder = { Text(placeholder) },
            trailingIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Fechar busca")
                }
            },
        )
    }
}

/** Results of an in-document search; an empty list for a non-blank term shows "Nenhum resultado". */
@Composable
fun DoctrineSearchResults(
    results: List<DoctrineSearchResult>,
    onResultClick: (DoctrineSearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (results.isEmpty()) {
        DoctrineEmptyResults(modifier)
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(results, key = { it.key() }) { result ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onResultClick(result) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                when (result) {
                    is DoctrineSearchResult.Question -> {
                        Text(
                            text = "Pergunta ${result.number}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(text = result.question, style = MaterialTheme.typography.bodyLarge)
                    }
                    is DoctrineSearchResult.Paragraph -> {
                        Text(
                            text = "Capítulo ${result.chapter} · parágrafo ${result.paragraph}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = result.preview,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

private fun DoctrineSearchResult.key(): String = when (this) {
    is DoctrineSearchResult.Question -> "q$number"
    is DoctrineSearchResult.Paragraph -> "p$chapter.$paragraph"
}
