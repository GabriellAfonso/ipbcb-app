package com.ipb.castelobranco.features.admin.profiles.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.ElasticPullToRefresh
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.features.admin.members.presentation.components.MemberAvatar
import com.ipb.castelobranco.features.admin.profiles.domain.model.ProfilesTab
import com.ipb.castelobranco.features.admin.profiles.presentation.state.ProfileRowUi
import com.ipb.castelobranco.features.admin.profiles.presentation.state.ProfilesEvent
import com.ipb.castelobranco.features.admin.profiles.presentation.state.ProfilesListUiState
import com.ipb.castelobranco.features.admin.profiles.presentation.viewmodel.ProfilesListViewModel

@Composable
fun ProfilesListScreen(
    viewModel: ProfilesListViewModel,
    onBack: () -> Unit,
    onOpenAccount: (String) -> Unit,
    onLeaveArea: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfilesEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is ProfilesEvent.LeaveArea -> onLeaveArea(event.message)
            }
        }
    }

    BaseScreen(tabName = "Perfis", showBackArrow = true, onBackClick = onBack) { innerPadding ->
        ProfilesListContent(
            state = state,
            imageLoader = viewModel.imageLoader,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.padding(innerPadding),
            onSelectTab = viewModel::selectTab,
            onRefresh = viewModel::refresh,
            onOpenAccount = onOpenAccount,
        )
    }
}

@Composable
fun ProfilesListContent(
    state: ProfilesListUiState,
    imageLoader: ImageLoader?,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onSelectTab: (ProfilesTab) -> Unit = {},
    onRefresh: () -> Unit = {},
    onOpenAccount: (String) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = state.tab.ordinal) {
            ProfilesTab.entries.forEach { tab ->
                Tab(
                    selected = tab == state.tab,
                    onClick = { onSelectTab(tab) },
                    text = { Text(tab.label()) },
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                ) {
                    Text(state.error, textAlign = TextAlign.Center)
                    Button(onClick = onRefresh) { Text("Tentar novamente") }
                }
                else -> ElasticPullToRefresh(isRefreshing = state.isRefreshing, onRefresh = onRefresh) {
                    AccountsList(state, imageLoader, onOpenAccount)
                }
            }
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun AccountsList(state: ProfilesListUiState, imageLoader: ImageLoader?, onOpenAccount: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.rows.isEmpty()) {
            item {
                Text(
                    text = if (state.tab == ProfilesTab.PENDING) "Nenhum pedido pendente." else "Nenhuma conta.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            }
        }
        items(state.rows, key = { it.id }) { row ->
            AccountRow(row, imageLoader, onClick = { onOpenAccount(row.id) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun AccountRow(row: ProfileRowUi, imageLoader: ImageLoader?, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        MemberAvatar(
            initials = row.initials,
            photoUrl = row.photoUrl,
            imageLoader = imageLoader,
            modifier = Modifier.size(44.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.pendingLabel?.let { Tag(it, MaterialTheme.colorScheme.primary) }
                if (row.isBlocked) Tag("Bloqueado", MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(disabledLabelColor = color),
    )
}

private fun ProfilesTab.label(): String = when (this) {
    ProfilesTab.PENDING -> "Pendentes"
    ProfilesTab.ALL -> "Todos"
}

@Preview(showBackground = true, widthDp = 390, heightDp = 700)
@Composable
private fun ProfilesListContentPreview() {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        ProfilesListContent(
            state = ProfilesListUiState(
                isLoading = false,
                rows = listOf(
                    ProfileRowUi("1", "Ana Souza", "AS", null, "Pedido de acesso", isBlocked = false),
                    ProfileRowUi("2", "Bruno Lima", "BL", null, null, isBlocked = true),
                ),
            ),
            imageLoader = null,
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
