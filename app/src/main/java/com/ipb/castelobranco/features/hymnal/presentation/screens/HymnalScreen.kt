package com.ipb.castelobranco.features.hymnal.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.ElasticPullToRefresh
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.hymnal.domain.model.Hymn
import com.ipb.castelobranco.features.hymnal.presentation.mapper.HymnGroup
import com.ipb.castelobranco.features.hymnal.presentation.viewmodel.HymnalViewModel

private val SheetRadius = 18.dp
private val CardRadius = 12.dp
private val CardSideMargin = 10.dp

// In sp so the column grows with the system font scale and three-digit numbers are never clipped.
private val NumberColumnWidth = 48.sp
private const val SEARCH_FIELD_ALPHA = 0.14f
private const val PLACEHOLDER_ALPHA = 0.7f
private const val COUNT_ALPHA = 0.75f
private const val RULE_ALPHA = 0.35f
private const val DIVIDER_ALPHA = 0.45f
private const val SEARCH_HINT = "Número, título ou trecho"

data class HymnalUiState(
    val hymns: List<Hymn> = emptyList(),
    val filteredHymns: List<Hymn> = emptyList(),
    /** [filteredHymns] split by hundred-ranges (see `groupHymnsByHundred`). */
    val groups: List<HymnGroup> = emptyList(),
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
        onBackClick = onBackClick,
        // The green behind the sheet's rounded corners continues the top bar.
        containerColor = ipbGreen,
        topBarExtension = {
            HymnSearchField(
                query = query,
                resultsCount = state.filteredHymns.size,
                onQueryChange = actions.onQueryChange,
            )
        },
    ) { innerPadding ->
        ElasticPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .clip(RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius))
                .background(MaterialTheme.colorScheme.surfaceDim)
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            if (state.groups.isEmpty()) {
                if (!state.isLoading) {
                    Text(
                        text = if (query.isBlank()) "Nenhum resultado" else "Nenhum hino encontrado para “$query”.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                HymnGroupsList(groups = state.groups, onHymnClick = actions.onHymnClick)
            }
        }
    }
}

@Composable
private fun HymnSearchField(
    query: String,
    resultsCount: Int,
    onQueryChange: (String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().background(ipbGreen)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = SEARCH_FIELD_ALPHA))
                .heightIn(min = 46.dp)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
                singleLine = true,
                cursorBrush = SolidColor(Color.White),
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(text = SEARCH_HINT, color = Color.White.copy(alpha = PLACEHOLDER_ALPHA), fontSize = 15.sp)
                    }
                    inner()
                },
            )
            if (query.isNotEmpty()) {
                Text(
                    text = if (resultsCount == 1) "1 hino" else "$resultsCount hinos",
                    color = Color.White.copy(alpha = COUNT_ALPHA),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Limpar busca", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun HymnGroupsList(groups: List<HymnGroup>, onHymnClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        groups.forEach { group ->
            item(key = "header-${group.label}") { GroupRule(group.label) }
            itemsIndexed(group.hymns, key = { _, hymn -> hymn.number }) { index, hymn ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CardSideMargin)
                        .clip(cardItemShape(index, group.hymns.lastIndex))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA))
                    }
                    HymnRow(item = hymn, onClick = { onHymnClick(hymn.number) })
                }
            }
        }
    }
}

@Composable
private fun GroupRule(label: String) {
    val rule = MaterialTheme.colorScheme.outline.copy(alpha = RULE_ALPHA)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = rule)
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = rule)
    }
}

@Composable
private fun HymnRow(
    item: Hymn,
    onClick: () -> Unit
) {
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
            .heightIn(min = 64.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = item.number,
            modifier = Modifier.width(with(LocalDensity.current) { NumberColumnWidth.toDp() }),
            color = BrandColors.Green,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (preview.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = preview,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun cardItemShape(index: Int, lastIndex: Int): RoundedCornerShape {
    val top = if (index == 0) CardRadius else 0.dp
    val bottom = if (index == lastIndex) CardRadius else 0.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}
