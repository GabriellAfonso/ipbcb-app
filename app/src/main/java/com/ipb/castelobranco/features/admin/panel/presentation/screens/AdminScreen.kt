package com.ipb.castelobranco.features.admin.panel.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.panel.domain.PanelCard
import com.ipb.castelobranco.features.admin.panel.presentation.navigation.AdminNav
import com.ipb.castelobranco.features.admin.panel.presentation.state.AdminPanelUiState
import com.ipb.castelobranco.features.admin.panel.presentation.viewmodel.AdminPanelViewModel

/**
 * Cores de destaque exclusivas dos cards do painel — não fazem parte da identidade visual
 * da igreja ([BrandColors]), servem só para diferenciar as áreas administrativas entre si.
 */
private val Blue = Color(0xFF2563EB)
private val Rose = Color(0xFFE11D48)
private val Indigo = Color(0xFF4F46E5)
private val Sky = Color(0xFF0EA5E9)
private val Pink = Color(0xFFDB2777)

/** Cinza aplicado no lugar do [AdminAction.accentColor] enquanto a ação não tem implementação. */
private val DisabledGray = Color(0xFF9CA3AF)

private const val CARD_CORNER_DP = 14
private const val ACCENT_BAR_WIDTH_DP = 4

/**
 * @param accentColor cor definitiva do card — continua declarada mesmo nas ações desligadas,
 *   então basta trocar [enabled] para `true` que o card volta sozinho para ela.
 * @param enabled `false` pinta o card de [DisabledGray] e torna o clique inerte.
 * @param badge selo numérico à direita do ícone; null não mostra nada.
 */
data class AdminAction(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val enabled: Boolean = true,
    val badge: String? = null,
    val onClick: () -> Unit
)

private const val PANEL_TITLE = "Painel de Gestão"
private const val EMPTY_PANEL_MESSAGE = "Nenhuma funcionalidade disponível para o seu perfil."

@Composable
fun AdminScreen(
    nav: AdminNav,
    viewModel: AdminPanelViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AdminPanelContent(state = state, nav = nav)
}

/**
 * Which cards appear comes from [state] (the user's access); how each card looks and where it goes
 * stays here.
 */
@Composable
fun AdminPanelContent(
    state: AdminPanelUiState,
    nav: AdminNav
) {
    val actions = state.cards.map { card -> card.toAction(nav, state.memberCount) }

    BaseScreen(
        tabName = PANEL_TITLE,
        logoRes = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick = nav.back,
        topBarExtension = { AdminHeaderStrip() }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Funcionalidades",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (state.isEmpty) {
                Text(
                    text = EMPTY_PANEL_MESSAGE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            actions.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { action ->
                        AdminActionCard(
                            action = action,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Cards com `enabled = false` estão cinzas por falta de implementação. Ao implementar, apague o
// `enabled = false` — o `accentColor` de cada um já é a cor definitiva do card e volta a valer sozinho.
internal fun PanelCard.toAction(nav: AdminNav, memberCount: Int?): AdminAction = when (this) {
    PanelCard.WORSHIP -> AdminAction(
        label = "Gestão do Louvor",
        description = "Músicas, domingos e cifras",
        icon = Icons.Filled.MusicNote,
        accentColor = BrandColors.Orange,
        onClick = nav.register
    )
    PanelCard.ATTENDANCE -> AdminAction(
        label = "Marcar Presença",
        description = "Presença dos membros",
        icon = Icons.Filled.Person,
        accentColor = BrandColors.Teal,
        enabled = false,
        onClick = { /* TODO */ }
    )
    PanelCard.SCHEDULE -> AdminAction(
        label = "Gerar Escala",
        description = "Criar escala mensal",
        icon = Icons.Filled.DateRange,
        accentColor = BrandColors.Green,
        onClick = nav.schedule
    )
    PanelCard.MEMBERS -> AdminAction(
        label = "Membros",
        description = "Gerenciar cadastro",
        icon = Icons.Filled.People,
        accentColor = Blue,
        badge = memberCount?.toString(),
        onClick = nav.members
    )
    PanelCard.NOTICES -> AdminAction(
        label = "Avisos",
        description = "Publicar comunicados",
        icon = Icons.AutoMirrored.Filled.Send,
        accentColor = Rose,
        enabled = false,
        onClick = { /* TODO */ }
    )
    PanelCard.REPORTS -> AdminAction(
        label = "Relatórios",
        description = "Estatísticas gerais",
        icon = Icons.Filled.BarChart,
        accentColor = Indigo,
        onClick = nav.reports
    )
    PanelCard.EVENTS -> AdminAction(
        label = "Eventos",
        description = "Criar e editar eventos",
        icon = Icons.Filled.Event,
        accentColor = Sky,
        enabled = false,
        onClick = { /* TODO */ }
    )
    PanelCard.NOTIFICATIONS -> AdminAction(
        label = "Notificações",
        description = "Enviar push notification",
        icon = Icons.Filled.Notifications,
        accentColor = Pink,
        enabled = false,
        onClick = { /* TODO */ }
    )
}

/**
 * Faixa colada logo abaixo da TopBar. O gradiente parte do próprio verde da TopBar
 * ([ipbGreen]) para o teal da marca, de modo que os dois leiam como um único bloco de
 * cabeçalho — o título fica na TopBar, aqui só sobra o subtítulo.
 */
@Composable
private fun AdminHeaderStrip() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(ipbGreen, BrandColors.Teal)
                )
            )
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Gerencie os recursos da igreja",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun AdminActionCard(
    action: AdminAction,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(CARD_CORNER_DP.dp)

    // Ação sem implementação usa cinza; a cor real fica guardada em action.accentColor e
    // volta a ser usada assim que a ação virar enabled = true.
    val accent = if (action.enabled) action.accentColor else DisabledGray
    val contentAlpha = if (action.enabled) 1f else 0.5f

    Box(
        modifier = modifier
            .padding(bottom = 10.dp)
            .clip(cardShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
            .border(1.dp, accent.copy(alpha = 0.18f), cardShape)
            .clickable(enabled = action.enabled, onClick = action.onClick)
    ) {
        // IntrinsicSize.Min dá altura concreta à Row para a barra de destaque poder
        // esticar até o fim do card — sem isso fillMaxHeight resolve para 0 dentro do scroll.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(ACCENT_BAR_WIDTH_DP.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(accent, accent.copy(alpha = 0.2f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(accent.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (action.badge != null) {
                        Spacer(modifier = Modifier.weight(1f))
                        CardBadge(text = action.badge, color = accent)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = action.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CardBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(7.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
