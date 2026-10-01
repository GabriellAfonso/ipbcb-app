package com.ipb.castelobranco.core.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.BirthdayEntry
import com.ipb.castelobranco.core.presentation.components.ElasticPullToRefresh
import com.ipb.castelobranco.core.presentation.components.PermissionErrorPlaceholder
import com.ipb.castelobranco.core.presentation.viewmodel.BirthdayMonthUi
import com.ipb.castelobranco.core.presentation.viewmodel.BirthdaysUiState
import com.ipb.castelobranco.core.presentation.viewmodel.BirthdaysViewModel

private const val MONTHS_PER_ROW = 2
private const val CAKE_EMOJI = "🎂"

@Composable
fun BirthdaysScreen(
    onBackClick: () -> Unit,
    onNavigateToAuth: () -> Unit,
    viewModel: BirthdaysViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    BirthdaysContent(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        onBackClick = onBackClick,
        onNavigateToAuth = onNavigateToAuth,
    )
}

@Composable
fun BirthdaysContent(
    uiState: BirthdaysUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    onNavigateToAuth: () -> Unit,
) {
    BaseScreen(
        tabName = "Aniversariantes",
        showBackArrow = true,
        onBackClick = onBackClick,
    ) { innerPadding ->
        ElasticPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.error != null -> PermissionErrorPlaceholder(
                    message = uiState.error,
                    onLoginClick = onNavigateToAuth,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    showLoginButton = uiState.showLoginButton,
                )

                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                else -> MonthGrid(
                    months = uiState.months,
                    currentMonth = uiState.currentMonth,
                    today = uiState.today,
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(months: List<BirthdayMonthUi>, currentMonth: Int, today: Int) {
    val rows = months.chunked(MONTHS_PER_ROW)
    // Created only once the months exist, so the initial index lands on the current month's row.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentMonth - 1) / MONTHS_PER_ROW)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(rows, key = { row -> row.first().month }) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { month ->
                    MonthCard(
                        month = month,
                        today = today,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthCard(month: BirthdayMonthUi, today: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val headerColor = if (month.isCurrent) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHighest
    val headerTextColor = if (month.isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.15f),
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerColor)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = CAKE_EMOJI, fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp))
            Text(
                text = month.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = headerTextColor,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            if (month.birthdays.isEmpty()) {
                Text(
                    text = "Nenhum aniversariante",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            month.birthdays.forEach { birthday ->
                BirthdayEntry(
                    birthday = birthday,
                    modifier = Modifier.padding(vertical = 3.dp),
                    badgeSize = 24.dp,
                    highlighted = month.isCurrent && birthday.day == today,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthGridPreview() {
    val names = listOf("Janeiro", "Fevereiro", "Março", "Abril")
    MonthGrid(
        months = names.mapIndexed { index, name ->
            val month = index + 1
            BirthdayMonthUi(
                month = month,
                name = name,
                birthdays = if (month == 3) emptyList() else listOf(
                    Birthday(name = "Ana Silva", month = month, day = 3),
                    Birthday(name = "Carlos Oliveira", month = month, day = 10),
                    Birthday(name = "Gabriela Martins de Albuquerque", month = month, day = 25),
                ).take(month),
                isCurrent = month == 2,
            )
        },
        currentMonth = 2,
        today = 10,
    )
}
