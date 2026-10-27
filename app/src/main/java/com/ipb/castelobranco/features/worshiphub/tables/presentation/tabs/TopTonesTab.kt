package com.ipb.castelobranco.features.worshiphub.tables.presentation.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopTone
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.PlayCount
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.PositionLabel
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.ProportionBar
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.RowDivider
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.TableRow
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.ToneChip
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.cardItem

@Composable
fun TopTonesTab(topTones: List<TopTone>) {
    val maxCount = topTones.maxOfOrNull { it.count }?.takeIf { it > 0 } ?: 1
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 10.dp, bottom = ListBottomPadding),
    ) {
        itemsIndexed(topTones) { index, tone ->
            Column(modifier = Modifier.cardItem(index, topTones.lastIndex)) {
                if (index > 0) RowDivider()
                TopTonesRow(index = index, tone = tone, fraction = tone.count.toFloat() / maxCount)
            }
        }
    }
}

@Composable
fun TopTonesRow(
    index: Int,
    tone: TopTone,
    fraction: Float,
) {
    TableRow {
        PositionLabel(position = index + 1, highlightTop = true)
        ToneChip(tone = tone.tone, large = true)
        ProportionBar(fraction = fraction, modifier = Modifier.weight(1f))
        PlayCount(count = tone.count)
    }
}
