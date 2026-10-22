package com.ipb.castelobranco.features.admin.schedule.presentation.components

import com.ipb.castelobranco.core.domain.util.normalize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.admin.schedule.domain.model.Member

private val Green = BrandColors.Green

@Composable
fun MemberSelectField(
    selectedMember: Member?,
    members: List<Member>,
    onMemberSelect: (Member) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery, members) {
        if (searchQuery.isBlank()) members
        else members.filter { it.name.normalize().contains(searchQuery.normalize(), ignoreCase = true) }
    }

    val triggerShape = if (expanded)
        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    else
        RoundedCornerShape(8.dp)

    Column(modifier = modifier) {

        // ── Trigger ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(triggerShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable { expanded = !expanded; if (!expanded) searchQuery = "" }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedMember?.name ?: "Responsável",
                style = MaterialTheme.typography.bodySmall,
                color = if (selectedMember != null)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = if (expanded) Green else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }

        // ── Dropdown ──────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                // Search field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(Green),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Buscar responsável...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                if (filtered.isEmpty()) {
                    Text(
                        text = "Nenhum membro encontrado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(12.dp)
                    )
                } else {
                    filtered.forEachIndexed { index, member ->
                        val isItemSelected = member.id == selectedMember?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isItemSelected) Green.copy(alpha = 0.08f) else Color.Transparent
                                )
                                .clickable {
                                    onMemberSelect(member)
                                    expanded = false
                                    searchQuery = ""
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isItemSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isItemSelected) Green else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (index < filtered.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
