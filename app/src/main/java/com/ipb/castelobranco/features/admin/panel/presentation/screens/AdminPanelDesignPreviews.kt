package com.ipb.castelobranco.features.admin.panel.presentation.screens

import java.time.LocalDate
import com.ipb.castelobranco.features.admin.panel.presentation.state.PendingConfirmationsUi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Green = Color(0xFF0F6B5C)
private val Orange = Color(0xFFF2A300)
private val Teal = Color(0xFF1A9E8A)
private val Blue = Color(0xFF2563EB)
private val Purple = Color(0xFF7C3AED)
private val Rose = Color(0xFFE11D48)
private val Indigo = Color(0xFF4F46E5)
private val Amber = Color(0xFFD97706)
private val Emerald = Color(0xFF059669)
private val Sky = Color(0xFF0EA5E9)
private val Slate = Color(0xFF475569)
private val Pink = Color(0xFFDB2777)

private data class AdminItem(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val badge: String? = null
)

private val allItems = listOf(
    AdminItem("Registrar Musica", "Adicionar nova musica", Icons.Filled.Edit, Orange),
    AdminItem("Marcar Presenca", "Registrar presenca dos membros", Icons.Filled.Person, Teal),
    AdminItem("Gerar Escala", "Criar escala mensal", Icons.Filled.DateRange, Green),
    AdminItem("Membros", "Gerenciar cadastro", Icons.Filled.People, Blue, badge = "142"),
    AdminItem("Financeiro", "Dizimos e ofertas", Icons.Filled.AccountBalance, Purple),
    AdminItem("Avisos", "Publicar comunicados", Icons.AutoMirrored.Filled.Send, Rose, badge = "3"),
    AdminItem("Relatorios", "Estatisticas gerais", Icons.Filled.BarChart, Indigo),
    AdminItem("Galeria", "Gerenciar fotos e albuns", Icons.Filled.PhotoLibrary, Amber),
    AdminItem("Eventos", "Criar e editar eventos", Icons.Filled.Event, Sky, badge = "2"),
    AdminItem("Grupos", "Celulas e ministerios", Icons.Filled.Groups, Emerald),
    AdminItem("Notificacoes", "Enviar push notification", Icons.Filled.Notifications, Pink),
    AdminItem("Configuracoes", "Ajustes do aplicativo", Icons.Filled.Settings, Slate),
)

// ═══════════════════════════════════════════════════════════════
// DESIGN A: Accent Border Cards
// Borda colorida na esquerda de cada card, visual premium
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Admin - Accent Border (Dark)")
@Composable
private fun DesignA_AccentBorder() {
    val bg = Color(0xFF0F172A)
    val surface = Color(0xFF1E293B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Green, Teal)))
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .statusBarsPadding()
        ) {
            Column {
                Text("Painel de Gestão", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Gerencie os recursos da igreja", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Acoes Rapidas",
            modifier = Modifier.padding(horizontal = 20.dp),
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBD5E1)
        )

        Spacer(Modifier.height(12.dp))

        val rows = allItems.chunked(2)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { item ->
                    Card(
                        modifier = Modifier.weight(1f).padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = surface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(80.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(item.color, item.color.copy(alpha = 0.3f))
                                        ),
                                        RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                                    )
                            )
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(item.icon, null, tint = item.color, modifier = Modifier.size(22.dp))
                                    if (item.badge != null) {
                                        Spacer(Modifier.weight(1f))
                                        Box(
                                            modifier = Modifier
                                                .background(item.color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                item.badge,
                                                fontSize = 10.sp,
                                                color = item.color,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    item.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0)
                                )
                                Text(
                                    item.description,
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
// DESIGN B: Gradient Glass Cards
// Cards com gradiente sutil da cor accent, efeito glass
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Admin - Gradient Glass (Dark)")
@Composable
private fun DesignB_GradientGlass() {
    val bg = Color(0xFF0F172A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Green, Teal)))
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .statusBarsPadding()
        ) {
            Column {
                Text("Painel de Gestão", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Gerencie os recursos da igreja", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Acoes Rapidas",
            modifier = Modifier.padding(horizontal = 20.dp),
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBD5E1)
        )

        Spacer(Modifier.height(12.dp))

        val rows = allItems.chunked(2)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        item.color.copy(alpha = 0.15f),
                                        Color(0xFF1E293B)
                                    )
                                )
                            )
                            .border(1.dp, item.color.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .clickable { }
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(item.color.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(item.icon, null, tint = item.color, modifier = Modifier.size(22.dp))
                                }
                                if (item.badge != null) {
                                    Spacer(Modifier.weight(1f))
                                    Box(
                                        modifier = Modifier
                                            .background(item.color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            item.badge,
                                            fontSize = 11.sp,
                                            color = item.color,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                item.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE2E8F0)
                            )
                            Text(
                                item.description,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
// DESIGN C: Mescla — Gradient Glass + Accent Border
// Gradiente sutil de fundo + borda esquerda colorida + borda fina
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Admin - Merged (Dark)")
@Composable
private fun DesignC_Merged() {
    val bg = Color(0xFF0F172A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Green.copy(alpha = 0.4f), Teal.copy(alpha = 0.3f))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .statusBarsPadding()
        ) {
            Column {
                Text("Painel de Gestão", color = Teal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Gerencie os recursos da igreja", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Acoes Rapidas",
            modifier = Modifier.padding(horizontal = 20.dp),
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBD5E1)
        )

        Spacer(Modifier.height(12.dp))

        val rows = allItems.chunked(2)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        item.color.copy(alpha = 0.12f),
                                        Color(0xFF1E293B)
                                    )
                                )
                            )
                            .border(1.dp, item.color.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .clickable { }
                    ) {
                        Row {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(item.color, item.color.copy(alpha = 0.2f))
                                        )
                                    )
                            )
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(item.color.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(item.icon, null, tint = item.color, modifier = Modifier.size(20.dp))
                                    }
                                    if (item.badge != null) {
                                        Spacer(Modifier.weight(1f))
                                        Box(
                                            modifier = Modifier
                                                .background(item.color.copy(alpha = 0.2f), RoundedCornerShape(7.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                item.badge,
                                                fontSize = 10.sp,
                                                color = item.color,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    item.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0)
                                )
                                Text(
                                    item.description,
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Preview(showBackground = true, widthDp = 390, name = "Admin - Pending Sundays")
@Composable
private fun PendingConfirmationsCardPreview() {
    PendingConfirmationsCard(
        pending = PendingConfirmationsUi.Dates(
            dates = listOf(LocalDate.of(2026, 10, 4), LocalDate.of(2026, 9, 27)),
            canDelete = true,
        ),
        onDateClick = {},
        onRetry = {},
    )
}
