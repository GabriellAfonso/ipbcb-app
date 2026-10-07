package com.ipb.castelobranco.core.presentation.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
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
        while (true) {
            yield()
            delay(autoScrollDuration)
            if (pages.isNotEmpty()) {
                val nextPage = (pagerState.currentPage + 1) % pages.size
                pagerState.animateScrollToPage(page = nextPage, animationSpec = tween(600))
            }
        }
    }

    val corner = 16.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            // 1. Sombra suave no container externo
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(corner),
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            )
            // 2. Container externo arredondado age como máscara (clip)
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            // 3. Cards internos são totalmente quadrados — sem clip, sem arredondamento
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                pages[page]()
            }
        }

        // 4. Efeito de borda interna sutil (opcional, substitui o drawInsetEdges)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val cr = corner.toPx()
                    val inset = 32f
                    val clip = Path().apply {
                        addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(cr, cr)))
                    }
                    clipPath(clip) {
                        drawInsetEdges(inset = inset, cornerPx = cr)
                    }
                }
        )
    }
}

private const val PAST_ROW_ALPHA = 0.45f

@Composable
fun HighlightSundaySchedule(schedule: NextScheduleUi, onClick: () -> Unit = {}) {
    val section = schedule.section
    val rows = section.rows.sortedBy { it.day }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {

        // Title + time
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (section.time.isNotBlank()) {
                Text(
                    text = section.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Schedule rows: each takes an equal share of the remaining height, so 4 or 5 rows fill the card
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            rows.forEachIndexed { index, row ->
                val isNext = row.day == schedule.nextDay
                val isPast = schedule.nextDay == null || row.day < schedule.nextDay
                val color = when {
                    isNext -> MaterialTheme.colorScheme.primary
                    isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = PAST_ROW_ALPHA)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(java.util.Locale.getDefault(), "%02d", row.day),
                        modifier = Modifier.weight(0.25f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold,
                        color = color
                    )
                    Text(
                        text = row.member,
                        modifier = Modifier.weight(0.75f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isNext) FontWeight.Bold else null,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (index != rows.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                }
            }
        }
    }
}
// ─── Default Composables ─────────────────────────────────────────────────────

@Composable
fun HighlightScheduleUnavailable(onClick: () -> Unit = {}) {
    HighlightPlaceholder(
        icon = "📅",
        title = "Escala",
        message = "Escala indisponível",
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun HighlightBirthdays(birthdays: List<Birthday>, onClick: () -> Unit = {}) {
    if (birthdays.isEmpty()) {
        HighlightPlaceholder(
            icon = "\uD83C\uDF82",
            title = "Aniversariantes do M\u00EAs",
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
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\uD83C\uDF82",
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "Aniversariantes do M\u00EAs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

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
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun HighlightEvents() {
    HighlightPlaceholder(
        icon = "📌",
        title = "Eventos",
        message = "Nenhum evento esse mês"
    )
}

@Composable
private fun HighlightPlaceholder(
    icon: String,
    title: String,
    message: String,
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
            color = Color(0xFFFFFFFF),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            color = Color(0xFF9E9E9E),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
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
            { HighlightEvents() }
        )
    )
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

// ─── Edge drawing ─────────────────────────────────────────────────────────────

private fun DrawScope.drawInsetEdges(inset: Float, cornerPx: Float) {
    val edgeColor = Color(0x2A000000)
    val transparent = Color.Transparent

    drawRoundRect(
        brush = Brush.verticalGradient(listOf(edgeColor, transparent), 0f, inset),
        topLeft = Offset(0f, 0f), size = Size(size.width, inset),
        cornerRadius = CornerRadius(cornerPx), blendMode = BlendMode.Multiply
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(transparent, edgeColor), size.height - inset, size.height),
        topLeft = Offset(0f, size.height - inset), size = Size(size.width, inset),
        cornerRadius = CornerRadius(cornerPx), blendMode = BlendMode.Multiply
    )
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(edgeColor, transparent), 0f, inset),
        topLeft = Offset(0f, 0f), size = Size(inset, size.height),
        cornerRadius = CornerRadius(cornerPx), blendMode = BlendMode.Multiply
    )
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(transparent, edgeColor), size.width - inset, size.width),
        topLeft = Offset(size.width - inset, 0f), size = Size(inset, size.height),
        cornerRadius = CornerRadius(cornerPx), blendMode = BlendMode.Multiply
    )
    drawRoundRect(
        brush = Brush.radialGradient(
            listOf(Color(0x14FFFFFF), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.5f),
            radius = size.minDimension * 0.75f
        ),
        cornerRadius = CornerRadius(cornerPx), blendMode = BlendMode.Screen
    )
}