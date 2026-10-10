package com.ipb.castelobranco.features.bible.presentation.screens

import com.ipb.castelobranco.core.domain.util.normalize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.bible.domain.model.BibleBook
import com.ipb.castelobranco.features.bible.presentation.viewmodel.BibleViewModel

private enum class IndexTab(val label: String, val key: String) {
    BOOK("Livro", "book"),
    CHAPTER("Capítulo", "chapter"),
    VERSE("Versículo", "verse");

    companion object {
        fun fromKey(key: String?): IndexTab =
            entries.firstOrNull { it.key == key } ?: BOOK
    }
}

@Composable
fun BibleIndexScreen(
    viewModel: BibleViewModel,
    initialTabKey: String,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(IndexTab.fromKey(initialTabKey)) }
    var pendingBookAbbrev by rememberSaveable { mutableStateOf(state.position.bookAbbrev) }
    var pendingChapter by rememberSaveable { mutableStateOf(state.position.chapter) }

    val pendingBook = remember(state.books, pendingBookAbbrev) {
        state.books.firstOrNull { it.abbrev.equals(pendingBookAbbrev, ignoreCase = true) }
            ?: state.books.firstOrNull()
    }

    BaseScreen(
        tabName = "Bíblia",
        showBackArrow = true,
        onBackClick = onBack,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                IndexTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.label,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                    )
                }
            }

            when (selectedTab) {
                IndexTab.BOOK -> BookList(
                    books = state.books,
                    isLoading = state.isLoadingBooks,
                    onBookClick = { book ->
                        pendingBookAbbrev = book.abbrev
                        pendingChapter = 1
                        selectedTab = IndexTab.CHAPTER
                    },
                )
                IndexTab.CHAPTER -> ChapterGrid(
                    book = pendingBook,
                    onChapterClick = { ch ->
                        pendingChapter = ch
                        selectedTab = IndexTab.VERSE
                    },
                )
                IndexTab.VERSE -> VerseGrid(
                    book = pendingBook,
                    chapter = pendingChapter,
                    onVerseClick = { verse ->
                        val abbrev = pendingBook?.abbrev ?: return@VerseGrid
                        viewModel.navigateTo(abbrev, pendingChapter, verse)
                        onBack()
                    },
                )
            }
        }
    }
}

@Composable
private fun BookList(
    books: List<BibleBook>,
    isLoading: Boolean,
    onBookClick: (BibleBook) -> Unit,
) {
    if (books.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) CircularProgressIndicator() else Text("Bíblia ainda não baixada.")
        }
        return
    }
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(books, searchQuery) {
        if (searchQuery.isBlank()) books
        else books.filter { it.name.normalize().contains(searchQuery.normalize(), ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Pesquisar livro...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum livro encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(filtered, key = { it.abbrev }) { book ->
                    BookRow(book = book, onClick = { onBookClick(book) })
                }
            }
        }
    }
}

@Composable
private fun BookRow(book: BibleBook, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = book.displayAbbrev,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = book.name,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "${book.chapterCount} cap.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChapterGrid(
    book: BibleBook?,
    onChapterClick: (Int) -> Unit,
) {
    if (book == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Selecione um livro primeiro.")
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items((1..book.chapterCount).toList()) { ch ->
            GridCell(text = ch.toString(), onClick = { onChapterClick(ch) })
        }
    }
}

@Composable
private fun VerseGrid(
    book: BibleBook?,
    chapter: Int,
    onVerseClick: (Int) -> Unit,
) {
    val verses = book?.chapter(chapter)
    if (book == null || verses == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Selecione um capítulo primeiro.")
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items((1..verses.size).toList()) { v ->
            GridCell(text = v.toString(), onClick = { onVerseClick(v) })
        }
    }
}


@Composable
private fun GridCell(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(56.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
