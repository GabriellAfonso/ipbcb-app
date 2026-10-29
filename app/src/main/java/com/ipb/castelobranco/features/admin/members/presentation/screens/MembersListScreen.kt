package com.ipb.castelobranco.features.admin.members.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.members.presentation.components.InvalidTag
import com.ipb.castelobranco.features.admin.members.presentation.components.MemberAvatar
import com.ipb.castelobranco.features.admin.members.presentation.components.StatusChip
import com.ipb.castelobranco.features.admin.members.presentation.state.MemberCardUi
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersListUiState
import com.ipb.castelobranco.features.admin.members.presentation.state.StatusFilterUi
import com.ipb.castelobranco.features.admin.members.presentation.viewmodel.MembersListViewModel

private const val BAND_TITLE = "Rol de membros"
private const val SEARCH_LABEL = "Buscar por nome"
private const val NO_MATCH = "Nenhum membro encontrado."
private const val BAND_KEY = "band"
private const val EMPTY_KEY = "empty"
private const val MUTED_ALPHA = 0.75f
private const val OUTLINE_ALPHA = 0.3f
private const val DIVIDER_ALPHA = 0.07f
private const val INVALID_ALPHA = 0.5f

private val BandCorner = 24.dp
private val CardCorner = 18.dp
private val AvatarSize = 46.dp
private val SearchHint = Color(0xFF3F4945)
private val SearchText = Color(0xFF191C1B)

@Composable
fun MembersListScreen(
    viewModel: MembersListViewModel,
    onBack: () -> Unit,
    onOpenMember: (Int) -> Unit,
    onAddMember: () -> Unit,
    onNavigationEvent: (MembersEvent) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MembersEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> onNavigationEvent(event)
            }
        }
    }

    BaseScreen(tabName = "Membros", showBackArrow = true, onBackClick = onBack) { innerPadding ->
        MembersListContent(
            state = state,
            imageLoader = viewModel.imageLoader,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.padding(innerPadding),
            onQueryChange = viewModel::onQueryChange,
            onStatusFilter = viewModel::onStatusFilter,
            onRetry = viewModel::refresh,
            onOpenMember = onOpenMember,
            onAddMember = onAddMember,
        )
    }
}

@Composable
fun MembersListContent(
    state: MembersListUiState,
    imageLoader: ImageLoader?,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenMember: (Int) -> Unit,
    onAddMember: () -> Unit,
    onStatusFilter: (String?) -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.error != null -> Message(
                text = state.error,
                action = "Tentar novamente",
                onAction = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
            else -> MembersList(state, imageLoader, onQueryChange, onStatusFilter, onOpenMember)
        }

        if (state.canAdd) {
            ExtendedFloatingActionButton(
                onClick = onAddMember,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Novo membro", fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(percent = 50),
                containerColor = ipbGreen,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** The band with the count, the search and the status filters, then one compact row per member (§7.3). */
@Composable
private fun MembersList(
    state: MembersListUiState,
    imageLoader: ImageLoader?,
    onQueryChange: (String) -> Unit,
    onStatusFilter: (String?) -> Unit,
    onOpenMember: (Int) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 96.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = BAND_KEY) { MembersBand(state, onQueryChange, onStatusFilter) }
        when {
            state.totalCount == 0 -> item(key = EMPTY_KEY) {
                Message("Nenhum membro cadastrado ainda. Toque em \"Novo membro\" para começar.")
            }
            state.members.isEmpty() -> item(key = EMPTY_KEY) { Message(NO_MATCH) }
            else -> itemsIndexed(state.members, key = { _, member -> member.id }) { index, member ->
                MemberRow(
                    member = member,
                    imageLoader = imageLoader,
                    isFirst = index == 0,
                    isLast = index == state.members.lastIndex,
                    onClick = { onOpenMember(member.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MembersBand(state: MembersListUiState, onQueryChange: (String) -> Unit, onStatusFilter: (String?) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = BandCorner, bottomEnd = BandCorner))
            .background(ipbGreen)
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
    ) {
        Text(BAND_TITLE, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = MUTED_ALPHA))
        Text(
            text = if (state.totalCount == 1) "1 cadastrado" else "${state.totalCount} cadastrados",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Spacer(Modifier.height(10.dp))
        SearchPill(state.query, onQueryChange)
        if (state.statusFilters.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.statusFilters.forEach { filter -> FilterChip(filter) { onStatusFilter(filter.status) } }
            }
        }
    }
}

@Composable
private fun SearchPill(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = ipbGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(SEARCH_LABEL, style = MaterialTheme.typography.bodyMedium, color = SearchHint)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = SearchText),
                cursorBrush = SolidColor(ipbGreen),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FilterChip(filter: StatusFilterUi, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Text(
        text = filter.label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (filter.isSelected) ipbGreen else Color.White,
        modifier = Modifier
            .clip(shape)
            .background(if (filter.isSelected) Color.White else Color.Transparent)
            .border(1.dp, if (filter.isSelected) Color.White else Color.White.copy(alpha = OUTLINE_ALPHA), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
    )
}

/** A member's line of the card: rounded on top for the first, at the bottom for the last. */
@Composable
private fun MemberRow(
    member: MemberCardUi,
    imageLoader: ImageLoader?,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(
        topStart = if (isFirst) CardCorner else 0.dp,
        topEnd = if (isFirst) CardCorner else 0.dp,
        bottomStart = if (isLast) CardCorner else 0.dp,
        bottomEnd = if (isLast) CardCorner else 0.dp,
    )
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = if (isFirst) 12.dp else 0.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(highlightCardColor()),
    ) {
        if (!isFirst) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = DIVIDER_ALPHA))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MemberAvatar(
                initials = member.initials,
                photoUrl = member.photoUrl,
                imageLoader = imageLoader,
                photoRevision = member.photoRevision,
                modifier = Modifier
                    .size(AvatarSize)
                    .alpha(if (member.isValid) 1f else INVALID_ALPHA),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(member.statusLabel)
                    if (!member.isValid) InvalidTag()
                }
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Message(
    text: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(24.dp),
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) Button(onClick = onAction) { Text(action) }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MembersListContentPreview() {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        MembersListContent(
            state = MembersListUiState(
                isLoading = false,
                canAdd = true,
                totalCount = 3,
                statusFilters = listOf(
                    StatusFilterUi(null, "Todos · 3", isSelected = true),
                    StatusFilterUi("Ativo", "Ativo · 1", isSelected = false),
                ),
                members = listOf(
                    MemberCardUi(1, "Ana Souza", "AS", null, "Ativo", isValid = true),
                    MemberCardUi(2, "Bruno Carvalho", "BC", null, "Inativo", isValid = false),
                    MemberCardUi(3, "Carla Mendes", "CM", null, "Visitante", isValid = true),
                ),
            ),
            imageLoader = null,
            snackbarHostState = remember { SnackbarHostState() },
            onQueryChange = {},
            onRetry = {},
            onOpenMember = {},
            onAddMember = {},
        )
    }
}
