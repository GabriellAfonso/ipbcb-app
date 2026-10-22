package com.ipb.castelobranco.features.worshiphub.hub.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.components.CustomButton
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.worshiphub.hub.presentation.navigation.WorshipHubNav

private const val EDGE_PADDING_RATIO = 0.041f
private const val GAP_RATIO = 0.127f

private data class WorshipHubButtonInfo(
    val iconRes: Int,
    val label: String,
    val onClick: () -> Unit,
    val visible: Boolean = true,
)

@Composable
fun WorshipHubScreen(
    nav: WorshipHubNav
) {
    val remembered = remember(nav) { nav }
    WorshipHubContent(actions = remembered)
}

@Composable
private fun WorshipHubContent(
    actions: WorshipHubNav
) {
    BaseScreen(
        tabName = "Min. Louvor",
        logoRes = R.drawable.ic_worshiphub,
        showBackArrow = true,
        onBackClick = actions.back
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WorshipHubButtonGrid(
                modifier = Modifier.fillMaxHeight(),
                actions = actions
            )
        }
    }
}

@Composable
private fun WorshipHubButtonGrid(
    modifier: Modifier = Modifier,
    actions: WorshipHubNav
) {
    val buttons = remember(actions) {
        listOf(
            WorshipHubButtonInfo(R.drawable.ic_table, "Tabelas", actions.tables),
            WorshipHubButtonInfo(R.drawable.ic_songs, "Músicas", actions.songs),
            WorshipHubButtonInfo(R.drawable.ic_chord_chart, "Cifras", actions.button3),
            WorshipHubButtonInfo(R.drawable.ic_lyrics, "Letras", actions.button4),
            WorshipHubButtonInfo(R.drawable.ic_in_development, "In Dev", actions.button5, visible = false),
            WorshipHubButtonInfo(R.drawable.ic_in_development, "In Dev", actions.button6, visible = false),
        )
    }

    BoxWithConstraints(modifier = modifier) {
        val edgePadding = maxWidth * EDGE_PADDING_RATIO
        val gap = maxWidth * GAP_RATIO
        val buttonSize = (maxWidth - edgePadding * 2 - gap) / 2

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = edgePadding),
            verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(3) { rowIndex ->
                val left = buttons[rowIndex * 2]
                val right = buttons[rowIndex * 2 + 1]

                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomButton(
                        image = painterResource(id = left.iconRes),
                        text = left.label,
                        onClick = if (left.visible) left.onClick else { {} },
                        modifier = Modifier.alpha(if (left.visible) 1f else 0f),
                        size = buttonSize,
                        playSoundOnClick = left.visible,
                    )
                    CustomButton(
                        image = painterResource(id = right.iconRes),
                        text = right.label,
                        onClick = if (right.visible) right.onClick else { {} },
                        modifier = Modifier.alpha(if (right.visible) 1f else 0f),
                        size = buttonSize,
                        playSoundOnClick = right.visible,
                    )
                }
            }
        }
    }
}
