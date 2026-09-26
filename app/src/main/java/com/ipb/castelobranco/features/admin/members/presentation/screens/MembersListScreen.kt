package com.ipb.castelobranco.features.admin.members.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.features.admin.members.presentation.components.MemberCard
import com.ipb.castelobranco.features.admin.members.presentation.state.MemberCardUi
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersListUiState
import com.ipb.castelobranco.features.admin.members.presentation.viewmodel.MembersListViewModel

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
            else -> MembersGrid(state, imageLoader, onQueryChange, onOpenMember)
        }

        ExtendedFloatingActionButton(
            onClick = onAddMember,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Novo membro") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun MembersGrid(
    state: MembersListUiState,
    imageLoader: ImageLoader?,
    onQueryChange: (String) -> Unit,
    onOpenMember: (Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text("Buscar por nome") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when {
            state.totalCount == 0 -> item(span = { GridItemSpan(maxLineSpan) }) {
                Message("Nenhum membro cadastrado ainda. Toque em \"Novo membro\" para começar.")
            }
            state.members.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                Message("Nenhum membro encontrado para \"${state.query}\".")
            }
            else -> items(state.members, key = { it.id }) { member ->
                MemberCard(member = member, imageLoader = imageLoader, onClick = { onOpenMember(member.id) })
            }
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
                totalCount = 3,
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
