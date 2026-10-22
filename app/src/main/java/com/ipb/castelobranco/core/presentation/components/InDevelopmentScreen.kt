package com.ipb.castelobranco.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen

@Composable
fun InDevelopmentScreen(
    onBack: () -> Unit
) {
    BaseScreen(
        tabName = "Em breve", // Título genérico para a TopBar
        logoRes = R.drawable.ic_in_development,
        showBackArrow = true,
        onBackClick = onBack,
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))
                Spacer(modifier = Modifier.height(24.dp))

                Icon(
                    painter = painterResource(id = R.drawable.ic_in_development),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Página em construção",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Esta funcionalidade estará disponível em breve.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))
                Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
            }
        }
    }
}