package com.ipb.castelobranco.features.studies.presentation.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp

/** Top-bar actions of every doctrine reader: search and the "Aa" text-size toggle. */
@Composable
fun DoctrineReaderActions(
    onSearchClick: () -> Unit,
    onFontSizeClick: (() -> Unit)?,
) {
    IconButton(onClick = onSearchClick) {
        Icon(Icons.Filled.Search, contentDescription = "Buscar")
    }
    if (onFontSizeClick != null) {
        IconButton(onClick = onFontSizeClick) {
            Text(text = "Aa", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        }
    }
}

/** "Ir para nº…" dialog; validation happens in the ViewModel. */
@Composable
fun GoToNumberDialog(
    lastNumber: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ir para a pergunta") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { value -> input = value.filter { it.isDigit() }.take(MAX_DIGITS) },
                singleLine = true,
                label = { Text("Número (1 a $lastNumber)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(input) }) { Text("Ir") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private const val MAX_DIGITS = 4
