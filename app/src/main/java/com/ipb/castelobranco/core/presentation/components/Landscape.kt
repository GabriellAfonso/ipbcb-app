package com.ipb.castelobranco.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.theme.ipbGreen

// Shared pieces of the landscape layouts (`specs/core/spec.md` §6.6).

/** Below this screen height (Material's compact height: a phone lying down) screens switch to the landscape layout. */
const val COMPACT_HEIGHT_DP = 480

private val PanelPadding = 16.dp
private val PillProgressSize = 16.dp
private val PillPadding = PaddingValues(horizontal = 16.dp)

/** True on a phone lying down. A tablet standing up keeps the portrait layout. */
@Composable
fun isCompactHeight(): Boolean = LocalConfiguration.current.screenHeightDp < COMPACT_HEIGHT_DP

/**
 * The green band of a screen, turned into a column on the left when the phone lies down. It scrolls on its own if
 * its content does not fit the height.
 */
@Composable
fun LandscapeSidePanel(
    width: Dp,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(ipbGreen)
            .verticalScroll(rememberScrollState())
            .padding(PanelPadding),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * The white pill a form puts in the top bar when the phone lies down, in place of its bottom save bar. While
 * [isLoading], a spinner replaces the label.
 */
@Composable
fun TopBarPillButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        contentPadding = PillPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = ipbGreen,
            disabledContainerColor = Color.White.copy(alpha = DISABLED_PILL_ALPHA),
            disabledContentColor = ipbGreen.copy(alpha = DISABLED_PILL_ALPHA),
        ),
        modifier = Modifier.padding(end = 4.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(PillProgressSize), strokeWidth = 2.dp, color = ipbGreen)
        } else {
            Text(text = label, fontWeight = FontWeight.SemiBold)
        }
    }
}

private const val DISABLED_PILL_ALPHA = 0.6f

@Preview(widthDp = 780, heightDp = 360)
@Composable
private fun LandscapeSidePanelPreview() {
    Row {
        LandscapeSidePanel(width = 220.dp) {
            Text("Marcos Vinícius Lima", color = Color.White)
            Text("Comungante · Diácono", color = Color.White)
        }
        Row(modifier = Modifier.background(ipbGreen).padding(8.dp)) {
            TopBarPillButton(label = "Salvar", onClick = {})
            TopBarPillButton(label = "Salvar", onClick = {}, isLoading = true)
        }
    }
}
