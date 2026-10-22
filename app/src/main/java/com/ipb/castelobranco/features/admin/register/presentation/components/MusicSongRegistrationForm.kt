package com.ipb.castelobranco.features.admin.register.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.admin.register.presentation.state.MusicSongFormState
import com.ipb.castelobranco.features.admin.register.presentation.screens.MusicSongRegistrationActions

private val Green = BrandColors.Green

@Composable
fun MusicSongRegistrationForm(
    state: MusicSongFormState,
    actions: MusicSongRegistrationActions,
    modifier: Modifier = Modifier
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Green,
        unfocusedBorderColor = Color.Transparent,
        focusedLabelColor = Green,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    )

    Column(modifier = modifier) {
        OutlinedTextField(
            value = state.title,
            onValueChange = actions.onTitleChange,
            label = { Text("Título") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.artist,
            onValueChange = actions.onArtistChange,
            label = { Text("Artista") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            )
        )
    }
}