package com.ipb.castelobranco.features.studies.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.studies.domain.model.BibleReference
import com.ipb.castelobranco.features.studies.domain.model.CatechismQuestion
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSearchResult
import com.ipb.castelobranco.features.studies.domain.model.ProofText
import com.ipb.castelobranco.features.studies.domain.model.VerseSpan
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineErrorView
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineFontSizeBar
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineLoadingView
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineReaderActions
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineSearchField
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineSearchResults
import com.ipb.castelobranco.features.studies.presentation.components.GoToNumberDialog
import com.ipb.castelobranco.features.studies.presentation.components.ProofTextChips
import com.ipb.castelobranco.features.studies.presentation.viewmodel.CatechismEvent
import com.ipb.castelobranco.features.studies.presentation.viewmodel.CatechismUiState
import com.ipb.castelobranco.features.studies.presentation.viewmodel.CatechismViewModel

data class CatechismActions(
    val onBackClick: () -> Unit,
    val onReferenceClick: (BibleReference) -> Unit,
    val onRetry: () -> Unit,
    val onGoTo: (String) -> Unit,
    val onOpenSearch: () -> Unit,
    val onCloseSearch: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onResultClick: (DoctrineSearchResult) -> Unit,
    val onIncreaseFont: () -> Unit,
    val onDecreaseFont: () -> Unit,
)

@Composable
fun CatechismScreen(
    viewModel: CatechismViewModel,
    onBackClick: () -> Unit,
    onReferenceClick: (BibleReference) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CatechismEvent.ScrollTo -> listState.scrollToItem(event.index)
                is CatechismEvent.ShowMessage -> Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    CatechismContent(
        state = state,
        listState = listState,
        actions = CatechismActions(
            onBackClick = onBackClick,
            onReferenceClick = onReferenceClick,
            onRetry = viewModel::retry,
            onGoTo = viewModel::goTo,
            onOpenSearch = viewModel::openSearch,
            onCloseSearch = viewModel::closeSearch,
            onQueryChange = viewModel::onQueryChange,
            onResultClick = viewModel::onResultClick,
            onIncreaseFont = viewModel::increaseFont,
            onDecreaseFont = viewModel::decreaseFont,
        ),
    )
}

@Composable
fun CatechismContent(
    state: CatechismUiState,
    listState: LazyListState,
    actions: CatechismActions,
) {
    var showFontBar by rememberSaveable { mutableStateOf(false) }
    var showGoTo by rememberSaveable { mutableStateOf(false) }

    BaseScreen(
        tabName = state.title.ifEmpty { "Doutrina" },
        showBackArrow = true,
        onBackClick = actions.onBackClick,
        extraActions = {
            if (state.questions.isNotEmpty()) {
                DoctrineReaderActions(
                    onSearchClick = actions.onOpenSearch,
                    onFontSizeClick = { showFontBar = !showFontBar },
                )
            }
        },
        topBarExtension = {
            when {
                state.isSearching -> DoctrineSearchField(
                    query = state.query,
                    placeholder = "Buscar no texto ou pelo número",
                    onQueryChange = actions.onQueryChange,
                    onClose = actions.onCloseSearch,
                )
                showFontBar -> DoctrineFontSizeBar(
                    fontSize = state.fontSize.size,
                    canDecrease = state.fontSize.canDecrease,
                    canIncrease = state.fontSize.canIncrease,
                    onDecrease = actions.onDecreaseFont,
                    onIncrease = actions.onIncreaseFont,
                )
            }
        },
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when {
            state.questions.isNotEmpty() -> {
                val results = state.results
                if (state.isSearching && results != null) {
                    DoctrineSearchResults(results = results, onResultClick = actions.onResultClick, modifier = modifier)
                } else {
                    Column(modifier = modifier.fillMaxSize()) {
                        QuestionList(
                            questions = state.questions,
                            fontSize = state.fontSize.size,
                            listState = listState,
                            onReferenceClick = actions.onReferenceClick,
                            modifier = Modifier.weight(1f),
                        )
                        CatechismFooter(
                            listState = listState,
                            total = state.questions.size,
                            firstNumber = state.questions.first().number,
                            onGoToClick = { showGoTo = true },
                        )
                    }
                }
            }
            state.isLoading -> DoctrineLoadingView(modifier)
            state.error != null -> DoctrineErrorView(message = state.error, onRetry = actions.onRetry, modifier = modifier)
            else -> DoctrineLoadingView(modifier)
        }
    }

    if (showGoTo && state.questions.isNotEmpty()) {
        GoToNumberDialog(
            lastNumber = state.questions.last().number,
            onConfirm = { input ->
                showGoTo = false
                actions.onGoTo(input)
            },
            onDismiss = { showGoTo = false },
        )
    }
}

@Composable
private fun QuestionList(
    questions: List<CatechismQuestion>,
    fontSize: Float,
    listState: LazyListState,
    onReferenceClick: (BibleReference) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        itemsIndexed(questions, key = { _, question -> question.number }) { _, question ->
            QuestionCard(question = question, fontSize = fontSize, onReferenceClick = onReferenceClick)
        }
    }
}

@Composable
private fun QuestionCard(
    question: CatechismQuestion,
    fontSize: Float,
    onReferenceClick: (BibleReference) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Text(
                text = "PERGUNTA ${question.number}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = question.question,
                modifier = Modifier.padding(top = 6.dp),
                fontSize = (fontSize - QUESTION_SIZE_DELTA).coerceAtLeast(MIN_QUESTION_SIZE).sp,
                lineHeight = (fontSize * QUESTION_LINE_HEIGHT).sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = question.answer,
                modifier = Modifier.padding(top = 12.dp),
                fontFamily = FontFamily.Serif,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * ANSWER_LINE_HEIGHT).sp,
            )
            if (question.proofTexts.isNotEmpty()) {
                Text(
                    text = "REFERÊNCIAS",
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ProofTextChips(proofTexts = question.proofTexts, onReferenceClick = onReferenceClick)
            }
        }
    }
}

@Composable
private fun CatechismFooter(
    listState: LazyListState,
    total: Int,
    firstNumber: Int,
    onGoToClick: () -> Unit,
) {
    val current by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex + firstNumber } }
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onGoToClick) { Text("Ir para nº…") }
                Text(
                    text = "Pergunta $current de $total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val QUESTION_SIZE_DELTA = 1f
private const val MIN_QUESTION_SIZE = 13f
private const val QUESTION_LINE_HEIGHT = 1.35f
private const val ANSWER_LINE_HEIGHT = 1.55f

@Preview(showBackground = true)
@Composable
private fun CatechismContentPreview() {
    MaterialTheme {
        CatechismContent(
            state = CatechismUiState(
                title = "Breve Catecismo",
                questions = listOf(
                    CatechismQuestion(
                        number = 1,
                        question = "Qual é o fim principal do homem?",
                        answer = "O fim principal do homem é glorificar a Deus, e gozá-lo para sempre.",
                        proofTexts = listOf(
                            ProofText("Rm 11.36", BibleReference("Rm", 11, listOf(VerseSpan(36, 36)))),
                            ProofText("Sl 86-8", null),
                        ),
                    ),
                ),
                isLoading = false,
            ),
            listState = rememberLazyListState(),
            actions = CatechismActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}
