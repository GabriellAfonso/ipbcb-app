package com.ipb.castelobranco.features.admin.members.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.features.admin.members.presentation.util.NO_MINISTRY

private val CardShape = RoundedCornerShape(20.dp)

@Composable
private fun Modifier.cardSurface(): Modifier {
    val colors = MaterialTheme.colorScheme
    return fillMaxWidth()
        .clip(CardShape)
        .background(colors.surfaceContainerLowest)
        .border(1.dp, colors.outlineVariant.copy(alpha = 0.6f), CardShape)
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.cardSurface().padding(horizontal = 18.dp, vertical = 6.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 13.sp,
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
        )
        content()
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Text(label, fontSize = 15.sp, color = colors.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MinistriesRow(ministries: List<String>) {
    val colors = MaterialTheme.colorScheme
    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
    ) {
        Text("Ministérios", fontSize = 15.sp, color = colors.onSurfaceVariant)
        if (ministries.isEmpty()) {
            Text(NO_MINISTRY, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ministries.forEach { ministry ->
                    Text(
                        text = ministry,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        modifier = Modifier
                            .border(1.dp, colors.outlineVariant, RoundedCornerShape(18.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/** "Perfil válido" — read-only; validity changes only in the form, so a stray tap never hides a member. */
@Composable
fun ValidityCard(isValid: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .cardSurface()
            .background(if (isValid) colors.surfaceContainerLowest else InvalidContainer)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (isValid) "Perfil válido" else INVALID_PROFILE,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isValid) colors.onSurface else OnInvalidContainer,
            )
            Text(
                text = if (isValid) {
                    "Aparece na lista de membros e nos aniversários."
                } else {
                    "Não aparece na lista de membros nem nos aniversários."
                },
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = if (isValid) colors.onSurfaceVariant else OnInvalidContainer,
            )
        }
    }
}

@Composable
fun HistoryCard(summary: String?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .cardSurface()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.secondaryContainer),
        ) {
            Icon(Icons.Filled.History, contentDescription = null, tint = colors.onSecondaryContainer)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "Histórico de alterações",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
            if (summary != null) {
                Text(summary, fontSize = 13.sp, color = colors.onSurfaceVariant, maxLines = 2)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.outline)
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileSectionsPreview() {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard("Vida na igreja") {
                InfoRow("Situação", "Visitante")
                InfoRow("Batismo", "Não informado")
                MinistriesRow(listOf("Louvor", "Recepção"))
            }
            ValidityCard(isValid = false)
            HistoryCard("Pr. João alterou Situação de Visitante para Ativo", onClick = {})
        }
    }
}
