package com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.BulletinHymn
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ServiceBulletin
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.util.WeekdayLabels
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.util.toFullDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One service, read like that Sunday's bulletin. No chart, because the question — "o que cantamos
 * no domingo passado" — is a list, not a magnitude.
 *
 * A card with no service name is weekday use outside any service, and says so rather than looking
 * like an unnamed service.
 */
@Composable
fun BulletinCard(
    bulletin: ServiceBulletin,
    modifier: Modifier = Modifier,
    onHymnClick: ((BulletinHymn) -> Unit)? = null,
) {
    val isService = bulletin.serviceName != null
    val accent = if (isService) BrandColors.Green else BrandColors.Orange

    val shape = RoundedCornerShape(CARD_CORNER.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(CARD_ELEVATION.dp, shape, spotColor = Color.Black.copy(alpha = SHADOW_ALPHA))
            .clip(shape)
            .background(highlightCardColor())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The date in a box, as on the schedule: "OUT" over "25".
            Column(
                modifier = Modifier
                    .size(width = 42.dp, height = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = bulletin.date.format(MONTH_ABBREVIATION).uppercase(PT_BR).take(MONTH_LETTERS),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = DAY_FORMAT.format(bulletin.date.dayOfMonth),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = bulletin.serviceName ?: "Fora de culto",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
                Text(
                    text = "${WeekdayLabels.full(bulletin.date.dayOfWeek)} · ${bulletin.date.toFullDate()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bulletin.hymns.forEach { hymn ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onHymnClick != null) {
                                Modifier.clickable { onHymnClick(hymn) }
                            } else {
                                Modifier
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${hymn.number} · ${hymn.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = "${hymn.deviceCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "${bulletin.hymns.size} hinos · alcance de ${bulletin.totalReach} aparelhos",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

private const val CARD_CORNER = 18
private const val CARD_ELEVATION = 6
private const val SHADOW_ALPHA = 0.12f
private const val MONTH_LETTERS = 3
private const val DAY_FORMAT = "%02d"
private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private val MONTH_ABBREVIATION: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", PT_BR)

@Preview(showBackground = true)
@Composable
private fun BulletinCardPreview() {
    BulletinCard(
        bulletin = ServiceBulletin(
            date = LocalDate.of(2026, 8, 16),
            serviceName = "Culto de Domingo à Noite",
            hymns = listOf(
                BulletinHymn("50", "Grandioso És Tu", 27),
                BulletinHymn("12", "Firme nas Promessas", 21),
            ),
            totalReach = 48,
        ),
        modifier = Modifier.padding(16.dp),
    )
}
