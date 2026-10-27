package com.ipb.castelobranco.features.bible.presentation.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.bible.domain.model.BibleBook
import com.ipb.castelobranco.features.bible.domain.model.BiblePlace
import com.ipb.castelobranco.features.bible.domain.model.label
import com.ipb.castelobranco.features.bible.presentation.viewmodel.BibleVideoPlacesEvent
import com.ipb.castelobranco.features.bible.presentation.viewmodel.BibleVideoPlacesUiState
import com.ipb.castelobranco.features.bible.presentation.viewmodel.BibleVideoPlacesViewModel

/** "Locais do vídeo": where one video shows. Managers only (reached from the videos sheet). */
@Composable
fun BibleVideoPlacesScreen(
    viewModel: BibleVideoPlacesViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                BibleVideoPlacesEvent.Saved -> onBack()
                is BibleVideoPlacesEvent.ShowMessage -> Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }
    BibleVideoPlacesContent(
        state = state,
        onBack = onBack,
        onRemove = viewModel::remove,
        onAdd = viewModel::add,
        onSave = viewModel::save,
    )
}

@Composable
private fun BibleVideoPlacesContent(
    state: BibleVideoPlacesUiState,
    onBack: () -> Unit,
    onRemove: (BiblePlace) -> Unit,
    onAdd: (List<BiblePlace>) -> Unit,
    onSave: () -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    BackHandler(enabled = picking) { picking = false }

    if (picking) {
        PlacePicker(
            books = state.books,
            existing = state.places.toSet(),
            onBack = { picking = false },
            onConfirm = { places ->
                picking = false
                onAdd(places)
            },
        )
        return
    }

    BaseScreen(
        tabName = "Locais do vídeo",
        showBackArrow = true,
        onBackClick = onBack,
        extraActions = {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(20.dp), strokeWidth = 2.dp)
            } else {
                TextButton(
                    onClick = { if (state.places.isEmpty()) confirmDelete = true else onSave() },
                    enabled = state.hasChanges,
                ) { Text("Salvar") }
            }
        },
    ) { padding ->
        val video = state.video
        if (video == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@BaseScreen
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            item(key = "video") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.SmartDisplay, contentDescription = null)
                        AsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                    Text(
                        text = video.title,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            items(state.places, key = { "${it.book}-${it.chapter}" }) { place ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = place.label(state.books),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    IconButton(onClick = { onRemove(place) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remover local")
                    }
                }
            }
            item(key = "add") {
                TextButton(
                    onClick = { picking = true },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Local", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir vídeo?") },
            text = { Text("Sem nenhum local, o vídeo é excluído.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onSave()
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

/** Book (with search), then several chapters and/or the whole book at once; any book, not only the one being read. */
@Composable
private fun PlacePicker(
    books: List<BibleBook>,
    existing: Set<BiblePlace>,
    onBack: () -> Unit,
    onConfirm: (List<BiblePlace>) -> Unit,
) {
    var bookAbbrev by rememberSaveable { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Set<BiblePlace>>(emptySet()) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val book = books.firstOrNull { it.abbrev == bookAbbrev }
    BackHandler(enabled = book != null) {
        bookAbbrev = null
        selected = emptySet()
    }

    BaseScreen(
        tabName = book?.name ?: "Escolher livro",
        showBackArrow = true,
        onBackClick = {
            if (book != null) {
                bookAbbrev = null
                selected = emptySet()
            } else {
                onBack()
            }
        },
        extraActions = {
            if (book == null && !isSearching) {
                IconButton(onClick = { isSearching = true }) {
                    Icon(Icons.Filled.Search, contentDescription = "Pesquisar livro")
                }
            }
        },
        topBarExtension = {
            if (book == null && isSearching) {
                BookSearchField(query = query, onQueryChange = { query = it }, onClose = {
                    isSearching = false
                    query = ""
                })
            }
        },
    ) { padding ->
        if (book == null) {
            Box(modifier = Modifier.padding(padding)) {
                BookList(
                    books = books,
                    isLoading = books.isEmpty(),
                    searchQuery = if (isSearching) query else "",
                    onBookClick = { bookAbbrev = it.abbrev },
                )
            }
            return@BaseScreen
        }
        val wholeBook = BiblePlace.of(book.abbrev, null)
        val toggle = { place: BiblePlace -> selected = if (place in selected) selected - place else selected + place }
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(CHAPTER_COLUMNS),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "whole", span = { GridItemSpan(maxLineSpan) }) {
                    FilterChip(
                        selected = wholeBook in selected || wholeBook in existing,
                        enabled = wholeBook !in existing,
                        onClick = { toggle(wholeBook) },
                        label = { Text("Livro inteiro", fontWeight = FontWeight.SemiBold) },
                    )
                }
                items((1..book.chapterCount).toList(), key = { it }) { chapter ->
                    val place = BiblePlace.of(book.abbrev, chapter)
                    GridCell(
                        text = chapter.toString(),
                        onClick = { if (place !in existing) toggle(place) },
                        isRead = place in existing,
                        isSelected = place in selected,
                        checkDescription = "Já é um local",
                    )
                }
            }
            Button(
                onClick = { onConfirm(selected.sortedBy { it.chapter ?: 0 }) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
            ) { Text("Adicionar (${selected.size})") }
        }
    }
}

private const val CHAPTER_COLUMNS = 5
