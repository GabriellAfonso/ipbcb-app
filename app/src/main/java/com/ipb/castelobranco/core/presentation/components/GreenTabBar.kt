package com.ipb.castelobranco.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.core.presentation.theme.onPrimaryContainerLight

private const val ACTIVE_TAB_ALPHA = 0.16f
private const val INACTIVE_TAB_ALPHA = 0.8f
private const val SUB_LABEL_ALPHA = 0.8f
private val BadgeTextColor = Color(0xFF2B1D00)

/**
 * One tab of [GreenTabBar]; [subLabel] is a small second line (e.g. the service time) and [badge] a count drawn
 * in an orange pill beside the label (e.g. pending requests).
 */
@Immutable
data class GreenTab(val label: String, val subLabel: String? = null, val badge: Int? = null)

/**
 * Tabs drawn inside the green top-bar block (pass it as `BaseScreen.topBarExtension`): white text, active tab on a
 * translucent pill with a mint underline. The content below is expected to be a sheet with rounded top corners.
 */
@Composable
fun GreenTabBar(
    tabs: List<GreenTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ipbGreen)
            .padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            val textColor = Color.White.copy(alpha = if (selected) 1f else INACTIVE_TAB_ALPHA)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color.White.copy(alpha = ACTIVE_TAB_ALPHA) else Color.Transparent)
                    .drawBehind {
                        if (selected) {
                            val stroke = 2.dp.toPx()
                            drawRect(
                                color = onPrimaryContainerLight,
                                topLeft = Offset(0f, size.height - stroke),
                                size = Size(size.width, stroke),
                            )
                        }
                    }
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AutoSizeTabText(
                            text = tab.label,
                            color = textColor,
                            maxLines = if (tab.label.contains(" ")) 2 else 1,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        )
                        tab.badge?.takeIf { it > 0 }?.let { TabBadge(it) }
                    }
                    tab.subLabel?.takeIf { it.isNotBlank() }?.let { sub ->
                        Text(
                            text = sub,
                            color = textColor.copy(alpha = textColor.alpha * SUB_LABEL_ALPHA),
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabBadge(count: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(BrandColors.Orange)
            .padding(horizontal = 6.dp),
    ) {
        Text(text = count.toString(), color = BadgeTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Shrinks the font until [text] fits, then draws (no flash of overflowing text). */
@Composable
private fun AutoSizeTabText(
    text: String,
    color: Color,
    maxLines: Int,
    fontWeight: FontWeight = FontWeight.Normal,
    maxFontSize: TextUnit = 13.sp,
    minFontSize: TextUnit = 9.sp,
    stepSize: TextUnit = 0.5.sp,
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = maxLines,
        textAlign = TextAlign.Center,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize > minFontSize) {
                fontSize = (fontSize.value - stepSize.value).coerceAtLeast(minFontSize.value).sp
            } else {
                readyToDraw = true
            }
        },
        modifier = Modifier.drawWithContent { if (readyToDraw) drawContent() },
    )
}

@Preview(name = "GreenTabBar — labels with time")
@Composable
private fun GreenTabBarPreview() {
    var selected by remember { mutableIntStateOf(0) }
    GreenTabBar(
        tabs = listOf(GreenTab("Terça", "19h30"), GreenTab("Quinta", "19h30"), GreenTab("Domingo", "18h")),
        selectedIndex = selected,
        onSelect = { selected = it },
    )
}

@Preview(name = "GreenTabBar — badge")
@Composable
private fun GreenTabBarBadgePreview() {
    GreenTabBar(
        tabs = listOf(GreenTab("Pendentes", badge = 2), GreenTab("Todos · 5")),
        selectedIndex = 0,
        onSelect = {},
    )
}

@Preview(name = "GreenTabBar — long labels")
@Composable
private fun GreenTabBarLongPreview() {
    GreenTabBar(
        tabs = listOf(
            GreenTab("Últimos Domingos"),
            GreenTab("Mais tocadas"),
            GreenTab("Top tons"),
            GreenTab("Repertório"),
        ),
        selectedIndex = 0,
        onSelect = {},
    )
}
