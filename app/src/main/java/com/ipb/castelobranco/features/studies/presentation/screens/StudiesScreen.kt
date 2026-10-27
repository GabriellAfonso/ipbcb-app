package com.ipb.castelobranco.features.studies.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.studies.presentation.components.StudiesEntryCard

@Composable
fun StudiesScreen(
    onBackClick: () -> Unit,
    onOpenDoctrine: () -> Unit,
    onOpenSundaySchool: () -> Unit,
) {
    BaseScreen(
        tabName = "Estudos",
        showBackArrow = true,
        onBackClick = onBackClick,
    ) { innerPadding ->
        StudiesContent(
            onOpenDoctrine = onOpenDoctrine,
            onOpenSundaySchool = onOpenSundaySchool,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun StudiesContent(
    onOpenDoctrine: () -> Unit,
    onOpenSundaySchool: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Aprenda e aprofunde sua fé com o material da igreja.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StudiesEntryCard(
            title = "Doutrina",
            subtitle = "Catecismos e Confissão de Westminster",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            onClick = onOpenDoctrine,
        )
        StudiesEntryCard(
            title = "Escola Bíblica Dominical",
            subtitle = "Aulas por ano",
            icon = Icons.Filled.School,
            onClick = onOpenSundaySchool,
        )
        StudiesEntryCard(
            title = "Personagens Bíblicos",
            subtitle = "Fichas, famílias e linha do tempo",
            icon = Icons.Filled.Person,
            onClick = null,
        )
        StudiesEntryCard(
            title = "História da Igreja",
            subtitle = "Da Reforma à IPB no Brasil",
            icon = Icons.Filled.History,
            onClick = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StudiesContentPreview() {
    MaterialTheme { StudiesContent(onOpenDoctrine = {}, onOpenSundaySchool = {}) }
}
