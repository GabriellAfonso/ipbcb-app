package com.ipb.castelobranco.features.admin.profiles.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.ElasticPullToRefresh
import com.ipb.castelobranco.core.presentation.components.GreenTab
import com.ipb.castelobranco.core.presentation.components.GreenTabBar
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.members.presentation.components.MemberAvatar
import com.ipb.castelobranco.features.admin.profiles.domain.model.ProfilesTab
import com.ipb.castelobranco.features.admin.profiles.presentation.components.RequestQuote
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

    BaseScreen(
        tabName = "Perfis",
        showBackArrow = true,
        onBackClick = onBack,
        // The green behind the sheet's rounded corners continues the top bar.
        containerColor = ipbGreen,
        topBarExtension = {
            GreenTabBar(
                tabs = state.tabs(),
                selectedIndex = state.tab.ordinal,
                onSelect = { viewModel.selectTab(ProfilesTab.entries[it]) },
            )
        },
    ) { innerPadding ->
        ProfilesListContent(
            state = state,
            imageLoader = viewModel.imageLoader,
            snackbarHostState = snackbarHostState,
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .clip(RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius))
                .background(MaterialTheme.colorScheme.surfaceDim)
                .padding(bottom = innerPadding.calculateBottomPadding()),
            onRefresh = viewModel::refresh,
            onOpenAccount = onOpenAccount,
        )
    }
}

/** The tab bar lives in the top bar; the content is the sheet below it. */
@Composable
fun ProfilesListContent(
    state: ProfilesListUiState,
    imageLoader: ImageLoader?,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onOpenAccount: (String) -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
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

@Composable
private fun AccountsList(state: ProfilesListUiState, imageLoader: ImageLoader?, onOpenAccount: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
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
            AccountCard(
                row = row,
                showRequest = state.tab == ProfilesTab.PENDING,
                imageLoader = imageLoader,
                onClick = { onOpenAccount(row.id) },
            )
        }
    }
}

@Composable
private fun AccountCard(row: ProfileRowUi, showRequest: Boolean, imageLoader: ImageLoader?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MemberAvatar(
                initials = row.initials,
                photoUrl = row.photoUrl,
                imageLoader = imageLoader,
                modifier = Modifier.size(44.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.name, style = MaterialTheme.typography.titleSmall)
                AccountTags(row)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showRequest && row.pendingLabel != null) {
            row.pendingMessage?.let { RequestQuote(it, Modifier.padding(top = 10.dp)) }
            row.pendingAt?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountTags(row: ProfileRowUi) {
    val colors = MaterialTheme.colorScheme
    val hasTag = row.pendingLabel != null || row.hasAccess || row.isBlocked
    if (!hasTag) {
        row.username?.let {
            Text("@$it", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        row.pendingLabel?.let {
            if (row.isCorrection) {
                Tag(it, colors.tertiaryContainer, colors.onTertiaryContainer)
            } else {
                Tag(it, colors.secondaryContainer, colors.onSecondaryContainer)
            }
        }
        if (row.hasAccess) Tag("Acesso liberado", ipbGreen, Color.White)
        if (row.isBlocked) Tag("Pedidos bloqueados", colors.errorContainer, colors.onErrorContainer)
    }
}

@Composable
private fun Tag(text: String, background: Color, content: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = content,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

private fun ProfilesListUiState.tabs(): List<GreenTab> = ProfilesTab.entries.map { tab ->
    when (tab) {
        ProfilesTab.PENDING -> GreenTab("Pendentes", badge = pendingCount)
        ProfilesTab.ALL -> GreenTab(allCount?.let { "Todos · $it" } ?: "Todos")
    }
}

private val SheetRadius = 18.dp
private val CardRadius = 18.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 700)
@Composable
private fun ProfilesListContentPreview() {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        ProfilesListContent(
            state = ProfilesListUiState(
                isLoading = false,
                rows = listOf(
                    ProfileRowUi(
                        "1", "Ana Souza", "AS", null, "Pedido de acesso", isBlocked = false,
                        pendingMessage = "Sou membro desde 2010, congrego no culto da noite.",
                        pendingAt = "29/10/2026 às 21:14",
                    ),
                    ProfileRowUi(
                        "2", "Rafael Nunes", "RN", null, "Pedido de correção", isBlocked = false,
                        hasAccess = true, isCorrection = true,
                        pendingMessage = "Meu sobrenome está errado no rol.", pendingAt = "28/10/2026 às 08:40",
                    ),
                ),
                pendingCount = 2,
                allCount = 5,
            ),
            imageLoader = null,
            snackbarHostState = remember { SnackbarHostState() },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceDim),
        )
    }
}
