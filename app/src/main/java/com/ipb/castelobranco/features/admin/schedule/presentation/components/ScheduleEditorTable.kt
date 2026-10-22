package com.ipb.castelobranco.features.admin.schedule.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.admin.schedule.domain.model.Member
import com.ipb.castelobranco.features.admin.schedule.presentation.state.EditableScheduleUiState

private val SORT_ORDER = listOf("terça", "terca", "quinta", "domingo")
private val PT_BR = java.util.Locale.forLanguageTag("pt-BR")

@Composable
fun ScheduleEditorTable(
    items: List<EditableScheduleUiState>,
    members: List<Member>,
    onMemberSelect: (itemIndex: Int, member: Member) -> Unit,
    modifier: Modifier = Modifier
) {
    val grouped = items
        .mapIndexed { index, item -> index to item }
        .groupBy { (_, item) -> item.scheduleTypeName }

    val sortedGroups = grouped.entries.sortedBy { (typeName, _) ->
        val lower = typeName.trim().lowercase(PT_BR)
        SORT_ORDER.indexOfFirst { lower.startsWith(it) }
            .let { if (it == -1) Int.MAX_VALUE else it }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sortedGroups.forEach { (typeName, indexedItems) ->
            ScheduleEditorSection(
                typeName = typeName,
                indexedItems = indexedItems,
                members = members,
                onMemberSelect = onMemberSelect
            )
        }
    }
}

@Composable
private fun ScheduleEditorSection(
    typeName: String,
    indexedItems: List<Pair<Int, EditableScheduleUiState>>,
    members: List<Member>,
    onMemberSelect: (Int, Member) -> Unit
) {
    val container = MaterialTheme.colorScheme.surfaceContainerHighest
    val headerBg = MaterialTheme.colorScheme.surfaceDim
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(container, shape)
            .padding(8.dp)
    ) {
        Text(
            text = typeName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dia",
                modifier = Modifier.weight(0.2f),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "Responsável",
                modifier = Modifier.weight(0.8f),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        indexedItems.forEachIndexed { sectionIndex, (globalIndex, item) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(java.util.Locale.getDefault(), "%02d", item.day),
                    modifier = Modifier
                        .weight(0.2f)
                        .padding(start = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                MemberSelectField(
                    selectedMember = item.selectedMember,
                    members = members,
                    onMemberSelect = { onMemberSelect(globalIndex, it) },
                    modifier = Modifier.weight(0.8f)
                )
            }

            if (sectionIndex < indexedItems.lastIndex) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            }
        }
    }
}