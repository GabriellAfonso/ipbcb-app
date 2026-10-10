package com.ipb.castelobranco.features.bible.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.features.bible.domain.model.VerseRef
import com.ipb.castelobranco.features.bible.presentation.components.BibleDownloadBanner
import com.ipb.castelobranco.features.bible.presentation.components.BibleFontSizeBar
import com.ipb.castelobranco.features.bible.presentation.components.BiblePendingWifiBanner
import com.ipb.castelobranco.features.bible.presentation.components.BibleReaderToolbar
import com.ipb.castelobranco.features.bible.presentation.components.BibleVerseColumn
import com.ipb.castelobranco.features.bible.presentation.components.BibleWaitingForWifiBanner
import com.ipb.castelobranco.features.bible.presentation.components.VerseRowData
import com.ipb.castelobranco.features.bible.presentation.viewmodel.BibleUiEvent
import com.ipb.castelobranco.features.bible.presentation.viewmodel.BibleViewModel

@Composable
fun BibleReaderScreen(
    viewModel: BibleViewModel,
    onBack: () -> Unit,
    onOpenIndex: (tab: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnWifi by viewModel.isOnWifi.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var showFontBar by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BibleUiEvent.CopiedToClipboard -> {
                    copyToClipboard(context, event.text)
                    Toast.makeText(context, "Versículos copiados", Toast.LENGTH_SHORT).show()
                    viewModel.clearSelection()
                }
                is BibleUiEvent.ShareText -> {
                    shareText(context, event.text)
                    viewModel.clearSelection()
                }
                is BibleUiEvent.ShowMessage -> {
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                }
                BibleUiEvent.ScrollToCurrentVerse -> {
                    val verse = state.position.verse
                    val target = (verse - 1).coerceAtLeast(0) + 1 // +1 = item header
                    runCatching { listState.scrollToItem(target) }
                }
            }
        }
    }

    BackHandler(enabled = state.selectedVerses.isNotEmpty()) {
        viewModel.clearSelection()
    }

    val book = state.currentBook
    val download = state.download
    val showBanner = !state.isBibleReady ||
        download.isDownloading ||
        download.isPending

    Scaffold(
        topBar = {
            Column {
                BibleReaderToolbar(
                    bookName = book?.name.orEmpty(),
                    chapter = state.position.chapter,
                    activeTranslation = state.activeTranslation,
                    cachedTranslations = state.cachedTranslations,
                    onBack = onBack,
                    onBookClick = { onOpenIndex("book") },
                    onChapterClick = { onOpenIndex("chapter") },
                    onTranslationSelected = viewModel::setActiveTranslation,
                    onFontSizeClick = { showFontBar = !showFontBar },
                )
                if (showFontBar) {
                    BibleFontSizeBar(
                        fontSizeSp = state.fontSize,
                        onFontSizeChange = viewModel::setFontSize,
                    )
                }
            }
        },
        bottomBar = {
            if (state.selectedVerses.isNotEmpty()) {
                SelectionActionBar(
                    onCopy = viewModel::copySelected,
                    onShare = viewModel::shareSelected,
                    onCancel = viewModel::clearSelection,
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                download.isDownloading -> BibleDownloadBanner(download)
                download.isPending && !isOnWifi -> BibleWaitingForWifiBanner(
                    onDownloadWithMobileData = viewModel::retryDownloadOnMobileData,
                )
                download.isPending -> BiblePendingWifiBanner()
                else -> Unit
            }

            if (book == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when {
                        state.isLoadingBooks || download.isDownloading || download.isPending ->
                            CircularProgressIndicator()
                        download.error != null -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = download.error,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp),
                            )
                            TextButton(onClick = viewModel::retryDownloadWifi) { Text("Tentar novamente") }
                        }
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Bíblia ainda não baixada.")
                            Spacer(Modifier.padding(top = 12.dp))
                            TextButton(onClick = viewModel::retryDownloadWifi) { Text("Baixar via WiFi") }
                            TextButton(onClick = viewModel::retryDownloadOnMobileData) {
                                Text("Baixar com dados móveis")
                            }
                        }
                    }
                }
                return@Scaffold
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val verses = state.currentChapterVerses
                val rows = remember(verses, state.selectedVerses, state.position) {
                    verses.mapIndexed { idx, text ->
                        val ref = VerseRef(book.abbrev, state.position.chapter, idx + 1)
                        VerseRowData(ref = ref, text = text, isSelected = ref in state.selectedVerses)
                    }
                }
                BibleVerseColumn(
                    bookName = book.name,
                    chapter = state.position.chapter,
                    verses = rows,
                    fontSizeSp = state.fontSize,
                    onVerseClick = { ref -> viewModel.toggleVerseSelection(ref) },
                    listState = listState,
                    modifier = Modifier.fillMaxSize(),
                )

                // Setas flutuantes para navegar capítulos.
                ChapterChevrons(
                    onPrevious = viewModel::previousChapter,
                    onNext = viewModel::nextChapter,
                    listState = listState,
                    chapter = state.position.chapter,
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ChapterChevrons(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    listState: LazyListState,
    chapter: Int,
) {
    var previousIndex by remember { mutableIntStateOf(listState.firstVisibleItemIndex) }
    var previousOffset by remember { mutableIntStateOf(listState.firstVisibleItemScrollOffset) }
    var visible by remember { mutableStateOf(true) }

    // Capítulo mudou: reseta visibilidade sem deixar o scroll detector interferir.
    LaunchedEffect(chapter) {
        visible = true
        previousIndex = 0
        previousOffset = 0
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                visible = when {
                    index < previousIndex -> true
                    index > previousIndex -> false
                    offset < previousOffset -> true
                    offset > previousOffset -> false
                    else -> visible
                }
                previousIndex = index
                previousOffset = offset
            }
    }

    val chevronBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomStart),
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.padding(12.dp).size(53.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = chevronBackground),
        ) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Capítulo anterior")
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomEnd),
    ) {
        IconButton(
            onClick = onNext,
            modifier = Modifier.padding(12.dp).size(53.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = chevronBackground),
        ) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Próximo capítulo")
        }
    }
}

@Composable
private fun SelectionActionBar(
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onCancel: () -> Unit,
) {
    BottomAppBar {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Row {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = "Compartilhar")
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Bíblia", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar"))
}
