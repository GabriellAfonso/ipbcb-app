package com.ipb.castelobranco.features.admin.register.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.admin.register.presentation.state.SundaySongRowState
import com.ipb.castelobranco.core.domain.model.Song

@Composable
fun SundayRegistrationForm(
    availableSongs: List<Song>,
    rows: List<SundaySongRowState>,
    onSongSelect: (position: Int, song: Song) -> Unit,
    onToneChange: (position: Int, tone: String) -> Unit,
    onAddMoreClick: () -> Unit,
    onRemoveRowClick: (position: Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        rows.forEach { row ->
            SundaySongRow(
                availableSongs = availableSongs,
                state = row,
                onSongSelect = { s -> onSongSelect(row.position, s) },
                onToneChange = { t -> onToneChange(row.position, t) },
                onRemoveClick = if (row.position > 4) ({ onRemoveRowClick(row.position) }) else null
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = onAddMoreClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "+")
        }
    }
}