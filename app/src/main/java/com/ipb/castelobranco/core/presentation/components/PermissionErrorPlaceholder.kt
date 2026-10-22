package com.ipb.castelobranco.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PermissionErrorPlaceholder(
    message: String,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLoginButton: Boolean = true,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (showLoginButton) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onLoginClick) {
                Text("Conectar à sua conta")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionErrorPlaceholderPreview() {
    PermissionErrorPlaceholder(
        message = "Disponível apenas para membros.",
        onLoginClick = {},
    )
}
