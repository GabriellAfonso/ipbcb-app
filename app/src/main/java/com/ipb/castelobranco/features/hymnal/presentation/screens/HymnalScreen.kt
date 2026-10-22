// kotlin
package com.ipb.castelobranco.features.hymnal.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ipb.castelobranco.core.presentation.components.ElasticPullToRefresh
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.features.hymnal.domain.model.Hymn
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.ipb.castelobranco.features.hymnal.presentation.viewmodel.HymnalViewModel

data class HymnalUiState(
    val hymns: List<Hymn> = emptyList(),
    val filteredHymns: List<Hymn> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class HymnalActions(
    val onQueryChange: (String) -> Unit,
    val onHymnClick: (String) -> Unit,
)

@Composable
fun HymnalScreen(
    viewModel: HymnalViewModel,
    onHymnClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val actions = HymnalActions(
        onQueryChange = viewModel::onQueryChange,
        onHymnClick = onHymnClick
    )

    HymnalContent(
        state = state,
        query = query,
        actions = actions,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        onBackClick = onBackClick
    )
}

@Composable
fun HymnalContent(
    state: HymnalUiState,
    query: String = "",
    actions: HymnalActions,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onBackClick: () -> Unit
) {

    BaseScreen(
        tabName = "Hinário",
        logoRes = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick = onBackClick
    ) { innerPadding ->

        ElasticPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
//            Spacer(modifier = Modifier.height(12.dp))

            val filteredHymns = state.filteredHymns

            SearchCard(
                query = query,
                onQueryChange = actions.onQueryChange,
                resultsCount = filteredHymns.size,
                onSearchClick = {}
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredHymns.isEmpty()) {
                // estado vazio
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Nenhum resultado",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredHymns, key = { it.number }) { item ->
                        HymnRow(
                            item = item,
                            onClick = { actions.onHymnClick(item.number) }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun SearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    resultsCount: Int,
    onSearchClick: () -> Unit,
) {
    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val green = BrandColors.Green

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    singleLine = true,
                    placeholder = { Text(text = "") },
                    trailingIcon = {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Pesquisar",
                                tint = green
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.inverseSurface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.inverseSurface,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.size(12.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Resultados:  $resultsCount",
                color = Color(0xFF2F7E6F),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun HymnRow(
    item: Hymn,
    onClick: () -> Unit
) {
    val green = BrandColors.Green
    val orange = BrandColors.Orange

    val preview = remember(item) {
        item.lyrics.firstOrNull()?.text
            ?.replace("\n", " ")
            ?.trim()
            .orEmpty()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(width = 4.dp, height = 34.dp)
                .background(orange, RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.size(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${item.number} \u2022 ${item.title}",
                color = green,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = preview,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
