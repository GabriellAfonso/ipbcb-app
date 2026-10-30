package com.ipb.castelobranco.core.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.ElasticPullToRefresh
import com.ipb.castelobranco.core.presentation.components.PermissionErrorPlaceholder
import com.ipb.castelobranco.core.presentation.components.access.MembersOnlyPlaceholder
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.core.presentation.viewmodel.BirthdayDayUi
import com.ipb.castelobranco.core.presentation.viewmodel.BirthdayMonthUi
import com.ipb.castelobranco.core.presentation.viewmodel.BirthdaysHeaderUi
import com.ipb.castelobranco.core.presentation.viewmodel.BirthdaysUiState
import com.ipb.castelobranco.core.presentation.viewmodel.BirthdaysViewModel
import com.ipb.castelobranco.core.presentation.viewmodel.UpcomingBirthdayUi

private const val MONTHS_PER_ROW = 2
private const val CAKE_EMOJI = "🎂"
private const val PAST_ALPHA = 0.5f
private const val BAND_SECONDARY_ALPHA = 0.8f
private const val BAND_SURFACE_ALPHA = 0.1f
private const val BAND_BORDER_ALPHA = 0.16f
private val CardRadius = 16.dp
private val OnOrange = Color(0xFF2B1D00)

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
                uiState.error != null && uiState.isMembersOnly -> MembersOnlyPlaceholder(
                    message = uiState.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )

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
                    header = uiState.header,
                    currentMonth = uiState.currentMonth,
                )
            }
        }
    }
}

/** The band scrolls with the grid; the list still opens on the current month's row. */
@Composable
private fun MonthGrid(months: List<BirthdayMonthUi>, header: BirthdaysHeaderUi?, currentMonth: Int) {
    val rows = months.chunked(MONTHS_PER_ROW)
    val headerItems = if (header != null) 1 else 0
    // Created only once the months exist, so the initial index lands on the current month's row.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = headerItems + (currentMonth - 1) / MONTHS_PER_ROW,
    )

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (header != null) {
            item(key = "header") { TodayBand(header) }
        }
        items(rows, key = { row -> row.first().month }) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { month ->
                    MonthCard(
                        month = month,
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
private fun TodayBand(header: BirthdaysHeaderUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(ipbGreen)
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 18.dp),
    ) {
        Text(
            text = header.dateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = BAND_SECONDARY_ALPHA),
        )
        Text(
            text = if (header.todayNames.isEmpty()) "Aniversariantes" else "Hoje tem aniversário",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )
        if (header.todayNames.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = BAND_SURFACE_ALPHA))
                    .border(1.dp, Color.White.copy(alpha = BAND_BORDER_ALPHA), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandColors.Orange),
                ) { Text(CAKE_EMOJI, fontSize = 20.sp) }
                Column {
                    Text(
                        text = "Hoje",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = BAND_SECONDARY_ALPHA),
                    )
                    Text(
                        text = header.todayNames.joinToString(" e "),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }
        if (header.upcoming.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .horizontalScroll(rememberScrollState()),
            ) {
                header.upcoming.forEach { UpcomingChip(it) }
            }
        }
    }
}

@Composable
private fun UpcomingChip(upcoming: UpcomingBirthdayUi) {
    Column(
        modifier = Modifier
            .widthIn(min = 96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = BAND_SURFACE_ALPHA))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(
            text = upcoming.dateLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Text(upcoming.firstName, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}

@Composable
private fun MonthCard(month: BirthdayMonthUi, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(CardRadius)
    val outline = if (month.isCurrent) Modifier.border(2.dp, ipbGreen, shape) else Modifier

    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(outline)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                text = month.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (month.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (month.birthdays.isEmpty()) {
            Text(
                text = "Nenhum aniversariante",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        month.birthdays.forEach { DayRow(it) }
    }
}

@Composable
private fun DayRow(birthday: BirthdayDayUi) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (birthday.isPast) PAST_ALPHA else 1f),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (birthday.isToday) BrandColors.Orange else colors.secondaryContainer),
        ) {
            Text(
                text = "%02d".format(birthday.day),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (birthday.isToday) OnOrange else colors.onSecondaryContainer,
            )
        }
        Text(
            text = birthday.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun MonthGridPreview() {
    val names = listOf("Janeiro", "Fevereiro", "Março", "Abril")
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        MonthGrid(
            months = names.mapIndexed { index, name ->
                val month = index + 1
                BirthdayMonthUi(
                    month = month,
                    name = name,
                    birthdays = if (month == 3) emptyList() else listOf(
                        BirthdayDayUi("Ana Silva", 3, isPast = month <= 2, isToday = false),
                        BirthdayDayUi("Carlos Oliveira", 10, isPast = month == 1, isToday = month == 2),
                        BirthdayDayUi("Gabriela Martins de Albuquerque", 25, isPast = month == 1, isToday = false),
                    ).take(month),
                    isCurrent = month == 2,
                )
            },
            header = BirthdaysHeaderUi(
                dateLabel = "Terça, 10 de fevereiro",
                todayNames = listOf("Carlos Oliveira"),
                upcoming = listOf(
                    UpcomingBirthdayUi("25/02 · qua", "Gabriela"),
                    UpcomingBirthdayUi("03/04 · sex", "Ana"),
                    UpcomingBirthdayUi("10/04 · sex", "Carlos"),
                ),
            ),
            currentMonth = 2,
        )
    }
}
