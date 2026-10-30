package com.ipb.castelobranco.core.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.domain.model.Gender
import com.ipb.castelobranco.features.schedule.presentation.components.NextScheduleUi
import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleRowUi
import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleSectionUi

// ─── Main Highlight Carousel ────────────────────────────────────────────────

@Composable
fun Highlight(
    pages: List<@Composable () -> Unit>,
    autoScrollDuration: Long = 5000L
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(Unit) {
        // Leaving the screen mid-animation cancels it, and the saved state keeps the partial offset.
        if (pagerState.currentPageOffsetFraction != 0f) {
            pagerState.scrollToPage(pagerState.currentPage)
        }
        while (true) {
            yield()
            delay(autoScrollDuration)
            // pageCount, not pages.size: this effect outlives the first list, and event pages come and go.
            val pageCount = pagerState.pageCount
            if (pageCount > 0) {
                val nextPage = (pagerState.currentPage + 1) % pageCount
                pagerState.animateScrollToPage(page = nextPage, animationSpec = tween(600))
            }
        }
    }

    val shape = RoundedCornerShape(HighlightCorner)

    // On a short screen the card shrinks, keeping 3:2, and the page dots still fit below it. No fillMaxWidth on the
    // card: a fixed width would make aspectRatio ignore the height left, and the card would spill over its neighbors.
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                // Event flyers are cropped to 3:2 on upload; a fixed ratio also keeps the card from flattening on
                // wide screens.
                .aspectRatio(HIGHLIGHT_ASPECT_RATIO)
                .shadow(
                    elevation = HighlightElevation,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = SHADOW_AMBIENT_ALPHA),
                    spotColor = Color.Black.copy(alpha = SHADOW_SPOT_ALPHA)
                )
                .clip(shape)
                .background(highlightCardColor()),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                // Pages are plain rects: the card's clip rounds the corners.
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    pages[page]()
                }
            }
        }

        if (pagerState.pageCount > 1) {
            HighlightPageIndicator(pageCount = pagerState.pageCount, currentPage = pagerState.currentPage)
        }
    }
}

private const val HIGHLIGHT_ASPECT_RATIO = 3f / 2f
private val HighlightCorner = 20.dp
private val HighlightElevation = 12.dp
private const val SHADOW_AMBIENT_ALPHA = 0.08f
private const val SHADOW_SPOT_ALPHA = 0.12f
private const val DARK_SURFACE_LUMINANCE = 0.5f

private val DotSize = 6.dp
private val ActiveDotWidth = 18.dp
private val DotGap = 6.dp

/** The card stands out from the screen: white on the light background, a lighter gray on the dark one. */
@Composable
fun highlightCardColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.surface.luminance() < DARK_SURFACE_LUMINANCE) scheme.surfaceContainerHigh
    else scheme.surfaceContainerLowest
}

@Composable
private fun HighlightPageIndicator(pageCount: Int, currentPage: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DotGap, Alignment.CenterHorizontally)
    ) {
        repeat(pageCount) { page ->
            val isCurrent = page == currentPage
            val width by animateDpAsState(if (isCurrent) ActiveDotWidth else DotSize, label = "dotWidth")
            Box(
                modifier = Modifier
                    .size(width = width, height = DotSize)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

/** Title (with an optional leading emoji) and an optional chip, over a thin divider. */
@Composable
private fun HighlightHeader(title: String, leading: String? = null, chip: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Text(text = leading, fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!chip.isNullOrBlank()) {
            Text(
                text = chip,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = CHIP_ALPHA))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = DIVIDER_ALPHA))
}

private const val CHIP_ALPHA = 0.06f
private const val DIVIDER_ALPHA = 0.08f
private const val PAST_ROW_ALPHA = 0.45f

private const val OWN_ROW_SUFFIX = " · você"

/** @param ownMemberId the logged-in user's member record: their row gets " · você" (`specs/core/spec.md` §6.3). */
@Composable
fun HighlightSundaySchedule(schedule: NextScheduleUi, ownMemberId: Long? = null, onClick: () -> Unit = {}) {
    val section = schedule.section
    val rows = section.rows.sortedBy { it.day }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {

        HighlightHeader(title = section.title, chip = section.time)

        // Schedule rows: each takes an equal share of the remaining height, so 4 or 5 rows fill the card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            rows.forEach { row ->
                val isNext = row.day == schedule.nextDay
                val isPast = schedule.nextDay == null || row.day < schedule.nextDay
                val color = when {
                    isNext -> MaterialTheme.colorScheme.primary
                    isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = PAST_ROW_ALPHA)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                val rowModifier = if (isNext) {
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    Modifier
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .then(rowModifier)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(java.util.Locale.getDefault(), "%02d", row.day),
                        modifier = Modifier.weight(0.25f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold,
                        color = color
                    )
                    val isOwn = ownMemberId != null && row.memberId == ownMemberId
                    Text(
                        text = if (isOwn) row.member + OWN_ROW_SUFFIX else row.member,
                        modifier = Modifier.weight(0.75f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isNext) FontWeight.Bold else null,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
// ─── Default Composables ─────────────────────────────────────────────────────

private const val SCHEDULE_ICON = "📅"
private const val SCHEDULE_TITLE = "Escala"
private const val BIRTHDAYS_ICON = "🎂"
private const val BIRTHDAYS_TITLE = "Aniversariantes do Mês"

@Composable
fun HighlightScheduleUnavailable(onClick: () -> Unit = {}) {
    HighlightPlaceholder(
        icon = SCHEDULE_ICON,
        title = SCHEDULE_TITLE,
        message = "Escala indisponível",
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/** Nothing loaded yet and the network may still bring it: a spinner instead of "unavailable". */
@Composable
fun HighlightScheduleLoading(onClick: () -> Unit = {}) {
    HighlightPlaceholder(
        icon = SCHEDULE_ICON,
        title = SCHEDULE_TITLE,
        message = null,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun HighlightBirthdaysLoading(onClick: () -> Unit = {}) {
    HighlightPlaceholder(
        icon = BIRTHDAYS_ICON,
        title = BIRTHDAYS_TITLE,
        message = null,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/** @param todayDay day of the month today: that birthday's badge is highlighted. */
@Composable
fun HighlightBirthdays(birthdays: List<Birthday>, todayDay: Int? = null, onClick: () -> Unit = {}) {
    if (birthdays.isEmpty()) {
        HighlightPlaceholder(
            icon = BIRTHDAYS_ICON,
            title = BIRTHDAYS_TITLE,
            message = "Nenhum aniversariante esse m\u00EAs",
            modifier = Modifier.clickable(onClick = onClick)
        )
        return
    }

    val columns = when {
        birthdays.size <= 5 -> 1
        birthdays.size <= 12 -> 2
        else -> 3
    }
    val badgeSize = when (columns) {
        1 -> 30.dp
        2 -> 26.dp
        else -> 22.dp
    }
    val badgeRadius = when (columns) {
        1 -> 8.dp
        else -> 6.dp
    }
    val verticalPad = when {
        birthdays.size <= 3 -> 6.dp
        birthdays.size <= 6 -> 4.dp
        birthdays.size <= 10 -> 3.dp
        else -> 2.dp
    }
    val nameStyle = when (columns) {
        1 -> MaterialTheme.typography.bodyMedium
        2 -> MaterialTheme.typography.bodySmall
        else -> MaterialTheme.typography.labelSmall
    }
    val badgeTextStyle = when (columns) {
        1 -> MaterialTheme.typography.labelLarge
        2 -> MaterialTheme.typography.labelMedium
        else -> MaterialTheme.typography.labelSmall
    }

    // Split birthdays across columns
    val columnItems = List(columns) { col ->
        val chunkSize = (birthdays.size + columns - 1) / columns
        birthdays.drop(col * chunkSize).take(chunkSize)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {
        HighlightHeader(title = BIRTHDAYS_TITLE, leading = BIRTHDAYS_ICON)

        // Adaptive grid
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            columnItems.forEach { colItems ->
                Column(modifier = Modifier.weight(1f)) {
                    colItems.forEach { birthday ->
                        BirthdayEntry(
                            birthday = birthday,
                            modifier = Modifier.padding(vertical = verticalPad),
                            badgeSize = badgeSize,
                            badgeRadius = badgeRadius,
                            badgeTextStyle = badgeTextStyle,
                            nameStyle = nameStyle,
                            highlighted = birthday.day == todayDay,
                        )
                    }
                }
            }
        }
    }
}


/** One carousel page per event (`specs/events/spec.md` §2): plain data, already formatted. */
data class HighlightEventUi(
    val id: Int,
    val title: String,
    val dateLabel: String,
    val imageUrl: String,
)

private const val EVENT_GRADIENT_START = 0.45f
private const val EVENT_GRADIENT_ALPHA = 0.75f
private const val EVENT_TITLE_MAX_LINES = 2

/**
 * The event's flyer fills the page; title and date sit over a dark gradient at the bottom. Without the
 * image (offline with no cache, refused, replaced) the neutral background keeps title and date legible.
 */
@Composable
fun HighlightEvent(
    page: HighlightEventUi,
    imageLoader: ImageLoader,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = page.imageUrl,
            imageLoader = imageLoader,
            contentDescription = page.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        EVENT_GRADIENT_START to Color.Transparent,
                        1f to Color.Black.copy(alpha = EVENT_GRADIENT_ALPHA),
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = page.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = EVENT_TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = page.dateLabel,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val LoadingIndicatorSize = 18.dp
private val LoadingIndicatorStroke = 2.dp

/** Icon and title over a message; no message means still loading, and a small spinner takes its place. */
@Composable
private fun HighlightPlaceholder(
    icon: String,
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 36.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (message == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(LoadingIndicatorSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                strokeWidth = LoadingIndicatorStroke,
            )
        } else {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = false)
@Composable
private fun HighlightPreview() {
    Highlight(
        pages = listOf(
            { HighlightScheduleUnavailable() },
            { HighlightBirthdays(birthdays = emptyList()) },
        )
    )
}

@Preview(showBackground = true, name = "Card - loading")
@Composable
private fun HighlightLoadingPreview() {
    Highlight(
        pages = listOf(
            { HighlightBirthdaysLoading() },
            { HighlightScheduleLoading() },
        )
    )
}

@Preview(showBackground = true, name = "Card - event (no image loaded)")
@Composable
private fun HighlightEventPreview() {
    val imageLoader = ImageLoader(LocalContext.current)
    Highlight(pages = listOf {
        HighlightEvent(
            page = HighlightEventUi(
                id = 1,
                title = "Congresso de Jovens",
                dateLabel = "14 a 16 nov · 19:30",
                imageUrl = "",
            ),
            imageLoader = imageLoader,
        )
    })
}

@Preview(showBackground = true, name = "Card - event, long title")
@Composable
private fun HighlightEventLongTitlePreview() {
    val imageLoader = ImageLoader(LocalContext.current)
    Highlight(pages = listOf {
        HighlightEvent(
            page = HighlightEventUi(
                id = 2,
                title = "Almoço de confraternização da família IPB Castelo Branco depois do culto de domingo",
                dateLabel = "30 nov",
                imageUrl = "",
            ),
            imageLoader = imageLoader,
        )
    })
}

@Preview(showBackground = true, name = "Card - schedule 4 rows (next = 9)")
@Composable
private fun HighlightSchedule4Preview() {
    Highlight(pages = listOf {
        HighlightSundaySchedule(
            schedule = NextScheduleUi(
                section = ScheduleSectionUi(
                    title = "Terça",
                    time = "19:30",
                    rows = listOf(
                        ScheduleRowUi(day = 2, member = "Ana Silva"),
                        ScheduleRowUi(day = 9, member = "Carlos Oliveira"),
                        ScheduleRowUi(day = 16, member = "Pedro Almeida"),
                        ScheduleRowUi(day = 23, member = "Juliana Costa"),
                    )
                ),
                nextDay = 9
            )
        )
    })
}

@Preview(showBackground = true, name = "Card - schedule 5 rows (next = 15)")
@Composable
private fun HighlightSchedule5Preview() {
    Highlight(pages = listOf {
        HighlightSundaySchedule(
            schedule = NextScheduleUi(
                section = ScheduleSectionUi(
                    title = "Domingo",
                    time = "18:00",
                    rows = listOf(
                        ScheduleRowUi(day = 1, member = "Ana Silva"),
                        ScheduleRowUi(day = 8, member = "Carlos Oliveira"),
                        ScheduleRowUi(day = 15, member = "Gabriela Martins de Albuquerque"),
                        ScheduleRowUi(day = 22, member = "Pedro Almeida"),
                        ScheduleRowUi(day = 29, member = "Juliana Costa"),
                    )
                ),
                nextDay = 15
            )
        )
    })
}

@Preview(showBackground = true, name = "Card - 4 birthdays (1 col)")
@Composable
private fun HighlightBirthdaysPreview() {
    Highlight(pages = listOf {
        HighlightBirthdays(
            birthdays = listOf(
                Birthday(name = "Ana Silva", month = 3, day = 3, gender = Gender.FEMALE),
                Birthday(name = "Carlos Oliveira", month = 3, day = 10, gender = Gender.MALE),
                Birthday(name = "Maria Santos", month = 3, day = 15, gender = Gender.FEMALE),
                Birthday(name = "Pedro Almeida", month = 3, day = 22, gender = Gender.MALE),
            )
        )
    })
}

@Preview(showBackground = true, name = "Card - empty")
@Composable
private fun HighlightBirthdaysEmptyPreview() {
    Highlight(pages = listOf { HighlightBirthdays(birthdays = emptyList()) })
}

@Preview(showBackground = true, name = "Card - 6 birthdays (2 cols)")
@Composable
private fun HighlightBirthdays6Preview() {
    Highlight(pages = listOf {
        HighlightBirthdays(
            birthdays = listOf(
                Birthday(name = "Ana Silva", month = 3, day = 1, gender = Gender.FEMALE),
                Birthday(name = "Carlos Oliveira", month = 3, day = 5, gender = Gender.MALE),
                Birthday(name = "Maria Santos", month = 3, day = 8, gender = Gender.FEMALE),
                Birthday(name = "Pedro Almeida", month = 3, day = 14, gender = Gender.MALE),
                Birthday(name = "Juliana Costa", month = 3, day = 20, gender = Gender.FEMALE),
                Birthday(name = "Fernando Souza", month = 3, day = 27, gender = Gender.MALE),
            )
        )
    })
}

@Preview(showBackground = true, name = "Card - 10 birthdays (2 cols + same day)")
@Composable
private fun HighlightBirthdays10Preview() {
    Highlight(pages = listOf {
        HighlightBirthdays(
            birthdays = listOf(
                Birthday(name = "Ana Silva", month = 3, day = 1, gender = Gender.FEMALE),
                Birthday(name = "Carlos Oliveira", month = 3, day = 3, gender = Gender.MALE),
                Birthday(name = "Dinalva Souza", month = 3, day = 3, gender = Gender.FEMALE),
                Birthday(name = "Maria Santos", month = 3, day = 5, gender = Gender.FEMALE),
                Birthday(name = "Pedro Almeida", month = 3, day = 8, gender = Gender.MALE),
                Birthday(name = "Juliana Costa", month = 3, day = 10, gender = Gender.FEMALE),
                Birthday(name = "Fernando Souza", month = 3, day = 10, gender = Gender.MALE),
                Birthday(name = "Beatriz Ferreira", month = 3, day = 17, gender = Gender.FEMALE),
                Birthday(name = "Gabriela Martins de Albuquerque", month = 3, day = 25, gender = Gender.FEMALE),
                Birthday(name = "Rafael Pereira", month = 3, day = 29, gender = Gender.MALE),
            )
        )
    })
}

@Preview(showBackground = true, name = "Card - 13 birthdays (3 cols)")
@Composable
private fun HighlightBirthdays13Preview() {
    Highlight(pages = listOf {
        HighlightBirthdays(
            birthdays = listOf(
                Birthday(name = "Ana paula", month = 3, day = 1, gender = Gender.FEMALE),
                Birthday(name = "Bruno perico arruda", month = 3, day = 2, gender = Gender.MALE),
                Birthday(name = "Carla", month = 3, day = 3, gender = Gender.FEMALE),
                Birthday(name = "Diego", month = 3, day = 5, gender = Gender.MALE),
                Birthday(name = "Elena", month = 3, day = 7, gender = Gender.FEMALE),
                Birthday(name = "Fabio", month = 3, day = 9, gender = Gender.MALE),
                Birthday(name = "Gisele", month = 3, day = 11, gender = Gender.FEMALE),
                Birthday(name = "Hugo", month = 3, day = 13, gender = Gender.MALE),
                Birthday(name = "Iris", month = 3, day = 15, gender = Gender.FEMALE),
                Birthday(name = "Jorge", month = 3, day = 18, gender = Gender.MALE),
                Birthday(name = "Karen", month = 3, day = 21, gender = Gender.FEMALE),
                Birthday(name = "Leo", month = 3, day = 24, gender = Gender.MALE),
                Birthday(name = "Marta", month = 3, day = 28, gender = Gender.FEMALE),
            )
        )
    })
}
