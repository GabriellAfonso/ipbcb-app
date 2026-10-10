package com.ipb.castelobranco.features.worshiphub.tables.presentation.screens

import com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel.RepertoireTexts
import com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel.RepertoireSaveState
import com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel.RepertoireEvent
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.ElasticPullToRefresh
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySet
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopSong
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopTone
import com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel.RepertoireRowState
import com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel.SongsTableViewModel
import com.ipb.castelobranco.features.worshiphub.tables.presentation.tabs.LastSundaysTab
import com.ipb.castelobranco.features.worshiphub.tables.presentation.tabs.RepertoireTab
import com.ipb.castelobranco.features.worshiphub.tables.presentation.tabs.TopSongsTab
import com.ipb.castelobranco.features.worshiphub.tables.presentation.tabs.TopTonesTab
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor

data class WorshipSongsUiState(
    val sundays: SnapshotState<List<SundaySet>> = SnapshotState.Loading,
    val topSongs: SnapshotState<List<TopSong>> = SnapshotState.Loading,
    val topTones: SnapshotState<List<TopTone>> = SnapshotState.Loading,
    val repertoireRows: List<RepertoireRowState> = emptyList(),
    val allSongs: List<Song> = emptyList(),
    val isRefreshingSuggestions: Boolean = false,
    val isRefreshing: Boolean = false,
    val save: RepertoireSaveState = RepertoireSaveState(),
)


data class WorshipSongsActions(
    val onBackClick: () -> Unit,
    val onGenerateClick: () -> Unit,
    val onClearRepertoire: () -> Unit,
    val onSaveClick: () -> Unit = {},
    val onConfirmSave: () -> Unit = {},
    val onDismissSave: () -> Unit = {},
    val onSongSelect: (position: Int, song: Song?) -> Unit,
    val onToneChange: (position: Int, tone: String) -> Unit,
    val onToggleFixed: (position: Int) -> Unit,
    val onRefreshCurrentTab: (tabIndex: Int) -> Unit = {},
    val onRepertoireOpen: () -> Unit = {},
    val onSongClick: (songId: Int) -> Unit = {},
)

@Composable
fun WorshipSongsTableScreen(
    onBackClick: () -> Unit,
    onSongClick: (songId: Int) -> Unit,
    viewModel: SongsTableViewModel
) {
    val state = WorshipSongsUiState(
        sundays = viewModel.lastSundays.collectAsStateWithLifecycle().value,
        topSongs = viewModel.topSongs.collectAsStateWithLifecycle().value,
        topTones = viewModel.topTones.collectAsStateWithLifecycle().value,
        repertoireRows = viewModel.repertoireRows.collectAsStateWithLifecycle().value,
        allSongs = viewModel.allSongs.collectAsStateWithLifecycle().value,
        isRefreshingSuggestions = viewModel.isRefreshingSuggestedSongs.collectAsStateWithLifecycle().value,
        isRefreshing = viewModel.isRefreshing.collectAsStateWithLifecycle().value,
        save = viewModel.saveState.collectAsStateWithLifecycle().value,
    )

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is RepertoireEvent.Saved -> event.message
                is RepertoireEvent.SaveFailed -> event.message
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val actions = WorshipSongsActions(
        onBackClick = onBackClick,
        onGenerateClick = viewModel::refreshSuggestedSongs,
        onClearRepertoire = viewModel::clearRepertoire,
        onSaveClick = viewModel::requestSave,
        onConfirmSave = viewModel::confirmSave,
        onDismissSave = viewModel::dismissSave,
        onSongSelect = viewModel::selectSong,
        onToneChange = viewModel::onToneChange,
        onToggleFixed = viewModel::toggleFixed,
        onRefreshCurrentTab = viewModel::refreshCurrentTab,
        onRepertoireOpen = viewModel::refreshAllSongs,
        onSongClick = onSongClick,
    )

    WorshipSongsTableContent(
        state = state,
        actions = actions,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun WorshipSongsTableContent(
    state: WorshipSongsUiState,
    actions: WorshipSongsActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    state.save.pendingSaveDate?.let { date ->
        AlertDialog(
            onDismissRequest = actions.onDismissSave,
            title = { Text(RepertoireTexts.confirmTitle(date)) },
            text = { Text(RepertoireTexts.CONFIRM_BODY) },
            confirmButton = { TextButton(onClick = actions.onConfirmSave) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = actions.onDismissSave) { Text("Cancelar") } },
        )
    }

    val tabs = listOf("Ultimos Domingos", "Mais tocadas", "Top tons", "Repertório")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val barColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val indicatorColor = MaterialTheme.colorScheme.secondary

    LaunchedEffect(showSearch) {
        if (showSearch) focusRequester.requestFocus()
    }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == REPERTOIRE_TAB_INDEX) actions.onRepertoireOpen()
    }

    BaseScreen(
        tabName = "Tabelas",
        logoRes = R.drawable.ic_table,
        showBackArrow = true,
        onBackClick = actions.onBackClick
    ) { innerPadding ->
        ElasticPullToRefresh(
            isRefreshing = state.isRefreshing,
            onRefresh    = { actions.onRefreshCurrentTab(selectedTabIndex) },
            modifier     = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.surfaceDim),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Barra de busca (aparece quando showSearch = true)
                AnimatedVisibility(
                    visible = showSearch,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(barColor)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface), // ← adiciona isso
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Buscar música, tom, artista, data...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                inner()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = barColor,
                    contentColor = Color.Black,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = indicatorColor,
                            height = 3.dp
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSingleWord = !title.contains(" ")
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                AutoSizeTabText(
                                    text = title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = if (isSingleWord) 1 else 2,
                                )
                            }
                        )
                    }
                }

                SelectionContainer {
                    when (selectedTabIndex) {
                        0 -> SnapshotContent(state.sundays) { data ->
                            LastSundaysTab(
                                sundays = data,
                                searchQuery = searchQuery,
                                onSongClick = actions.onSongClick,
                            )
                        }
                        1 -> SnapshotContent(state.topSongs) { data ->
                            TopSongsTab(
                                topSongs = data,
                                onSongClick = actions.onSongClick,
                            )
                        }
                        2 -> SnapshotContent(state.topTones) { data ->
                            TopTonesTab(topTones = data)
                        }
                        3 -> DisableSelection {
                            RepertoireTab(
                                rows = state.repertoireRows,
                                availableSongs = state.allSongs,
                                isRefreshing = state.isRefreshingSuggestions,
                                onSongSelect = actions.onSongSelect,
                                onToneChange = actions.onToneChange,
                                onToggleFixed = actions.onToggleFixed,
                                onGenerateClick = actions.onGenerateClick,
                                onClearClick = actions.onClearRepertoire,
                                save = state.save,
                                onSaveClick = actions.onSaveClick,
                                onSongInfoClick = actions.onSongClick,
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp),
            )

            // FAB de busca — sobreposto no canto inferior direito
            FloatingActionButton(
                onClick = {
                    if (showSearch) {
                        showSearch = false
                        searchQuery = ""
                    } else {
                        showSearch = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(
                    imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Buscar"
                )
            }
        }
    }
}

@Composable
private fun <T> SnapshotContent(
    state: SnapshotState<T>,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        is SnapshotState.Loading -> {}
        is SnapshotState.Data -> content(state.value)
        is SnapshotState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Erro ao carregar dados",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun AutoSizeTabText(
    text: String,
    color: Color,
    maxLines: Int,
    maxFontSize: TextUnit = 13.sp,
    minFontSize: TextUnit = 9.sp,
    stepSize: TextUnit = 0.5.sp,
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        maxLines = maxLines,
        textAlign = TextAlign.Center,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize > minFontSize) {
                fontSize = (fontSize.value - stepSize.value).coerceAtLeast(minFontSize.value).sp
            } else {
                readyToDraw = true
            }
        },
        modifier = Modifier.drawWithContent { if (readyToDraw) drawContent() },
    )
}

private const val REPERTOIRE_TAB_INDEX = 3
