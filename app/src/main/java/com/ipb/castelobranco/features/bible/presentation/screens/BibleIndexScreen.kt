package com.ipb.castelobranco.features.bible.presentation.screens

import com.ipb.castelobranco.core.domain.util.normalize
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.bible.domain.model.BibleBook
import com.ipb.castelobranco.features.bible.domain.model.Testament
import com.ipb.castelobranco.features.bible.domain.model.byTestament
import com.ipb.castelobranco.features.bible.domain.model.resolve
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

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val closeSearch = {
        isSearching = false
        searchQuery = ""
    }

    val pendingBook = remember(state.books, pendingBookAbbrev) { state.books.resolve(pendingBookAbbrev) }
    // Search only filters the book list: it belongs to that tab alone.
    val canSearch = selectedTab == IndexTab.BOOK && state.books.isNotEmpty()

    BackHandler(enabled = isSearching, onBack = closeSearch)

    BaseScreen(
        tabName = "Bíblia",
        showBackArrow = true,
        onBackClick = onBack,
        extraActions = {
            if (canSearch && !isSearching) {
                IconButton(onClick = { isSearching = true }) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Pesquisar livro")
                }
            }
        },
        topBarExtension = {
            if (canSearch && isSearching) {
                BookSearchField(query = searchQuery, onQueryChange = { searchQuery = it }, onClose = closeSearch)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                IndexTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == selectedTab,
                        onClick = {
                            closeSearch()
                            selectedTab = tab
                        },
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
                    searchQuery = if (isSearching) searchQuery else "",
                    onBookClick = { book ->
                        pendingBookAbbrev = book.abbrev
                        pendingChapter = 1
                        closeSearch()
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
    searchQuery: String,
    onBookClick: (BibleBook) -> Unit,
) {
    if (books.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) CircularProgressIndicator() else Text("Bíblia ainda não baixada.")
        }
        return
    }
    // Split before filtering: a book's testament comes from its place in the whole index.
    val rows = remember(books, searchQuery) {
        books.byTestament().flatMap { (testament, group) ->
            group
                .filter {
                    searchQuery.isBlank() || it.name.normalize().contains(searchQuery.normalize(), ignoreCase = true)
                }
                .map { it to testament }
        }
    }

    if (rows.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum livro encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(rows, key = { (book, _) -> book.abbrev }) { (book, testament) ->
            BookRow(book = book, testament = testament, onClick = { onBookClick(book) })
        }
    }
}

/** Circle and text colors of the book abbreviations: what tells the two testaments apart. */
@Composable
private fun Testament.colors(): Pair<Color, Color> = when (this) {
    Testament.OLD -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    Testament.NEW -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
}

/** Opened from the search button on the top bar; black so it stands apart from the list below. */
@Composable
private fun BookSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text("Pesquisar livro...") },
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Clear, contentDescription = "Fechar busca")
            }
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Black,
            unfocusedContainerColor = Color.Black,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedPlaceholderColor = Color.White.copy(alpha = SEARCH_HINT_ALPHA),
            unfocusedPlaceholderColor = Color.White.copy(alpha = SEARCH_HINT_ALPHA),
            focusedLeadingIconColor = Color.White,
            unfocusedLeadingIconColor = Color.White,
            focusedTrailingIconColor = Color.White,
            unfocusedTrailingIconColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

private const val SEARCH_HINT_ALPHA = 0.6f

@Composable
private fun BookRow(book: BibleBook, testament: Testament, onClick: () -> Unit) {
    val (circle, circleText) = testament.colors()
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
                .background(circle, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = book.displayAbbrev,
                style = MaterialTheme.typography.bodyMedium,
                color = circleText,
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
    val verseCount = book?.verseCount(chapter)
    if (book == null || verseCount == null) {
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
        items((1..verseCount).toList()) { v ->
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
