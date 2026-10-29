package com.ipb.castelobranco.features.admin.schedule.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.domain.util.normalize
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.panel.presentation.navigation.AdminNav
import com.ipb.castelobranco.features.admin.schedule.domain.model.Member
import com.ipb.castelobranco.features.admin.schedule.domain.model.ScheduleType
import com.ipb.castelobranco.features.admin.schedule.presentation.state.AdminScheduleEvent
import com.ipb.castelobranco.features.admin.schedule.presentation.state.AdminScheduleUiState
import com.ipb.castelobranco.features.admin.schedule.presentation.state.EditableScheduleUiState
import com.ipb.castelobranco.features.admin.schedule.presentation.state.SaveResult
import com.ipb.castelobranco.features.admin.schedule.presentation.viewmodel.AdminScheduleViewModel
import com.ipb.castelobranco.features.schedule.domain.formatter.MonthScheduleWhatsappFormatter
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val TITLE = "Escala"
private const val BAND_TITLE = "Escala de"
private const val PREVIOUS_MONTH = "Mês anterior"
private const val NEXT_MONTH = "Próximo mês"
private const val UNSAVED = "Alterações não salvas"
private const val SAVED = "Salva"
private const val SERVICE_TIME = "19:30"
private const val PICK_MEMBER = "Escolher responsável"
private const val SAME_WEEK = "Também escalado(a) nesta semana"
private const val PICKER_SUBTITLE = "Quem dirige esse culto?"
private const val SEARCH_LABEL = "Buscar pessoa"
private const val NOT_IN_MONTH = "Ainda não está no mês"
private const val CLEAR_MEMBER = "Deixar sem responsável"
private const val NO_MEMBER_FOUND = "Nenhuma pessoa encontrada"
private const val GENERATE_LABEL = "Gerar aleatória"
private const val SAVE_LABEL = "Salvar escala"
private const val SAVING_LABEL = "Salvando…"
private const val SHARE_LABEL = "Compartilhar"
private const val SHARE_TITLE = "Compartilhar escala"
private const val SHARE_SUBTITLE = "Texto que vai para o WhatsApp"
private const val SAVED_MESSAGE = "Escala salva"
private const val LEAVE_TITLE = "Trocar de mês?"
private const val LEAVE_STAY = "Ficar"
private const val LEAVE_GO = "Trocar sem salvar"
private const val DAY_FORMAT = "%02d"
private const val WEEKDAY_LETTERS = 3
private const val INITIALS = 2

private const val MUTED_ALPHA = 0.75f
private const val CHIP_ALPHA = 0.14f
private const val SHADOW_ALPHA = 0.12f
private const val DIVIDER_ALPHA = 0.07f

private val BandCorner = 24.dp
private val CardCorner = 18.dp
private val WarnBackground = Color(0xFFFDE6B3)
private val WarnText = Color(0xFF6B4300)
private val RepeatColor = Color(0xFF9A5B00)
private val SheetListMaxHeight = 420.dp
private val DASH = floatArrayOf(6f, 5f)

/** Fixed avatar colors, picked by member id. */
private val AvatarColors = listOf(
    Color(0xFF125B49), Color(0xFF1A9E8A), Color(0xFF504A7B), Color(0xFF9A5B00), Color(0xFF1565C0),
    Color(0xFF7B3F61), Color(0xFF2E6B2E), Color(0xFF8C3B3B), Color(0xFF3F4945),
)

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

// ── Actions ───────────────────────────────────────────────────────────────────

data class AdminScheduleActions(
    val onPreviousMonth: () -> Unit,
    val onNextMonth: () -> Unit,
    val onMemberSelect: (itemIndex: Int, member: Member) -> Unit,
    val onGenerate: () -> Unit,
    val onSave: () -> Unit,
    val onConfirmSave: () -> Unit,
    val onDismissSaveConfirmation: () -> Unit,
    val onShare: (String) -> Unit,
    val onDismissSaveResult: () -> Unit,
    val onMemberClear: (itemIndex: Int) -> Unit = {},
)

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun AdminScheduleScreen(
    nav: AdminNav,
    onShare: (String) -> Unit,
    viewModel: AdminScheduleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onEvent(AdminScheduleEvent.LoadMembers) }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.onEvent(AdminScheduleEvent.SnackbarShown)
    }

    val actions = AdminScheduleActions(
        onPreviousMonth = {
            val prev = if (state.month == 1) Pair(state.year - 1, 12)
                       else Pair(state.year, state.month - 1)
            viewModel.onEvent(AdminScheduleEvent.MonthChanged(prev.first, prev.second))
        },
        onNextMonth = {
            val next = if (state.month == 12) Pair(state.year + 1, 1)
                       else Pair(state.year, state.month + 1)
            viewModel.onEvent(AdminScheduleEvent.MonthChanged(next.first, next.second))
        },
        onMemberSelect = { index, member ->
            viewModel.onEvent(AdminScheduleEvent.MemberSelected(index, member))
        },
        onGenerate = { viewModel.onEvent(AdminScheduleEvent.GenerateSchedule) },
        onSave = { viewModel.onEvent(AdminScheduleEvent.SaveRequested) },
        onConfirmSave = { viewModel.onEvent(AdminScheduleEvent.SaveSchedule) },
        onDismissSaveConfirmation = { viewModel.onEvent(AdminScheduleEvent.SaveConfirmationDismissed) },
        onShare = onShare,
        onDismissSaveResult = { viewModel.onEvent(AdminScheduleEvent.SaveResultDismissed) },
        onMemberClear = { index -> viewModel.onEvent(AdminScheduleEvent.MemberCleared(index)) },
    )

    AdminScheduleContent(
        state = state,
        actions = actions,
        snackbarHostState = snackbarHostState,
        onBackClick = nav.back
    )
}

// ── Stateless screen ──────────────────────────────────────────────────────────

/** "Faixa e cards por culto" (`specs/admin/spec.md` §4). */
@Composable
fun AdminScheduleContent(
    state: AdminScheduleUiState,
    actions: AdminScheduleActions,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit
) {
    var pickerIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var showShare by rememberSaveable { mutableStateOf(false) }
    // The month change waiting for "Trocar sem salvar": -1 back, +1 forward.
    var pendingMonthStep by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(state.saveResult) {
        if (state.saveResult is SaveResult.Success) {
            actions.onDismissSaveResult()
            snackbarHostState.showSnackbar(SAVED_MESSAGE)
        }
    }

    val stepMonth: (Int) -> Unit = { step -> if (step < 0) actions.onPreviousMonth() else actions.onNextMonth() }

    BaseScreen(
        tabName = TITLE,
        logoRes = R.drawable.ic_schedule,
        showBackArrow = true,
        onBackClick = onBackClick
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    ScheduleBand(
                        state = state,
                        onMonthStep = { step ->
                            if (state.hasUnsavedChanges) pendingMonthStep = step else stepMonth(step)
                        },
                    )
                    ServiceCards(state, onRowClick = { pickerIndex = it })
                }
                ScheduleBottomBar(
                    state = state,
                    onGenerate = actions.onGenerate,
                    onSave = actions.onSave,
                    onShare = { showShare = true },
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }
    }

    val pickedItem = pickerIndex?.let { state.items.getOrNull(it) }
    if (pickerIndex != null && pickedItem != null) {
        val index = pickerIndex ?: 0
        MemberPickerSheet(
            item = pickedItem,
            members = state.members,
            counts = state.countsByMember,
            onPick = { member ->
                actions.onMemberSelect(index, member)
                pickerIndex = null
            },
            onClear = {
                actions.onMemberClear(index)
                pickerIndex = null
            },
            onDismiss = { pickerIndex = null },
        )
    }

    if (showShare) {
        val text = state.toWhatsappText()
        ShareSheet(
            text = text,
            onShare = {
                showShare = false
                actions.onShare(text)
            },
            onDismiss = { showShare = false },
        )
    }

    pendingMonthStep?.let { step ->
        AlertDialog(
            onDismissRequest = { pendingMonthStep = null },
            title = { Text(LEAVE_TITLE) },
            text = { Text("As alterações de ${state.monthLabel.substringBefore(' ')} ainda não foram salvas.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingMonthStep = null
                        stepMonth(step)
                    },
                ) { Text(LEAVE_GO) }
            },
            dismissButton = { TextButton(onClick = { pendingMonthStep = null }) { Text(LEAVE_STAY) } },
        )
    }

    if (state.showSaveConfirmation) {
        SaveConfirmationDialog(
            monthLabel = state.monthLabel,
            onConfirm = actions.onConfirmSave,
            onDismiss = actions.onDismissSaveConfirmation
        )
    }

    (state.saveResult as? SaveResult.Error)?.let { error ->
        AlertDialog(
            onDismissRequest = actions.onDismissSaveResult,
            title = { Text("Erro ao salvar") },
            text = { Text(error.message) },
            confirmButton = { TextButton(onClick = actions.onDismissSaveResult) { Text("OK") } },
        )
    }
}

// ── Private composables ───────────────────────────────────────────────────────

/** The month with its arrows, how much is filled and whether it was saved. */
@Composable
private fun ScheduleBand(state: AdminScheduleUiState, onMonthStep: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = BandCorner, bottomEnd = BandCorner))
            .background(ipbGreen)
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonthStep(-1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = PREVIOUS_MONTH,
                    tint = Color.White,
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = BAND_TITLE,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = MUTED_ALPHA),
                )
                Text(
                    text = state.monthLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
            IconButton(onClick = { onMonthStep(1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = NEXT_MONTH,
                    tint = Color.White,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BandChip(
                text = "${state.filledCount} de ${state.items.size} dias preenchidos",
                background = Color.White.copy(alpha = CHIP_ALPHA),
                color = Color.White,
            )
            when {
                state.hasUnsavedChanges -> BandChip(UNSAVED, WarnBackground, WarnText)
                state.isSaved -> BandChip(SAVED, Color.White, ipbGreen)
            }
        }
    }
}

@Composable
private fun BandChip(text: String, background: Color, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** One card per service type, Tuesday, Thursday then Sunday. */
@Composable
private fun ServiceCards(state: AdminScheduleUiState, onRowClick: (Int) -> Unit) {
    val counts = state.countsByMember
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScheduleType.entries.forEach { type ->
            val rows = state.items.withIndex().filter { it.value.scheduleTypeId == type.id }
            if (rows.isNotEmpty()) {
                ServiceCard(type.displayName, rows.size) {
                    rows.forEachIndexed { position, (index, item) ->
                        if (position > 0) RowDivider()
                        ServiceRow(
                            item = item,
                            count = item.selectedMember?.let { counts[it.id] } ?: 0,
                            repeated = state.isRepeatedThisWeek(index),
                            onClick = { onRowClick(index) },
                        )
                    }
                }
            }
        }
        if (state.isGenerating) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ipbGreen)
            }
        }
    }
}

@Composable
private fun ServiceCard(title: String, count: Int, rows: @Composable () -> Unit) {
    val shape = RoundedCornerShape(CardCorner)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, spotColor = Color.Black.copy(alpha = SHADOW_ALPHA))
            .clip(shape)
            .background(highlightCardColor()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(width = 4.dp, height = 16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$SERVICE_TIME · $count ${if (count == 1) "culto" else "cultos"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        rows()
    }
}

/** A service: the date box, who leads it and how often they lead this month. */
@Composable
private fun ServiceRow(item: EditableScheduleUiState, count: Int, repeated: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .size(width = 42.dp, height = 44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = item.weekdayAbbreviation(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = DAY_FORMAT.format(item.day),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.width(12.dp))
        val member = item.selectedMember
        Column(Modifier.weight(1f)) {
            if (member == null) {
                Text(
                    text = PICK_MEMBER,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (repeated) SAME_WEEK else "$count× no mês",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (repeated) RepeatColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (member == null) {
            val outline = MaterialTheme.colorScheme.outlineVariant
            Box(
                Modifier
                    .size(10.dp)
                    .drawBehind {
                        drawCircle(
                            color = outline,
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(DASH)),
                        )
                    },
            )
        } else {
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = DIVIDER_ALPHA))
}

/** The dice (generate) and "Salvar escala", or "Compartilhar" once the month is saved. */
@Composable
private fun ScheduleBottomBar(
    state: AdminScheduleUiState,
    onGenerate: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        RowDivider()
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable(
                        enabled = !state.isGenerating && !state.isSaving,
                        onClickLabel = GENERATE_LABEL,
                        onClick = onGenerate,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = ipbGreen)
                } else {
                    Icon(
                        imageVector = Icons.Filled.Casino,
                        contentDescription = GENERATE_LABEL,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            val sharing = state.canShare
            Button(
                onClick = if (sharing) onShare else onSave,
                enabled = sharing || (state.canSave && !state.isSaving && !state.isGenerating),
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ipbGreen,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
            ) {
                when {
                    sharing -> {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SHARE_LABEL, fontWeight = FontWeight.SemiBold)
                    }
                    state.isSaving -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(SAVING_LABEL, fontWeight = FontWeight.SemiBold)
                    }
                    else -> Text(SAVE_LABEL, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberPickerSheet(
    item: EditableScheduleUiState,
    members: List<Member>,
    counts: Map<Int, Int>,
    onPick: (Member) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val options = remember(members, counts, query) { membersByLeastUsed(members, counts, query) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "${item.scheduleTypeName}, ${item.dayMonth()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = PICKER_SUBTITLE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            SearchPill(query = query, onQueryChange = { query = it })
            Spacer(Modifier.height(8.dp))
            if (options.isEmpty()) {
                Text(
                    text = NO_MEMBER_FOUND,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                )
            }
            LazyColumn(Modifier.heightIn(max = SheetListMaxHeight)) {
                items(options, key = { it.id }) { member ->
                    MemberOption(
                        member = member,
                        count = counts[member.id] ?: 0,
                        isCurrent = member.id == item.selectedMember?.id,
                        onClick = { onPick(member) },
                    )
                }
                if (item.selectedMember != null) {
                    item {
                        Text(
                            text = CLEAR_MEMBER,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onClear)
                                .padding(horizontal = 4.dp, vertical = 14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchPill(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = SEARCH_LABEL,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MemberOption(member: Member, count: Int, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AvatarColors[member.id.mod(AvatarColors.size)]),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials(member.name),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (count == 0) NOT_IN_MONTH else "$count× no mês",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareSheet(text: String, onShare: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(SHARE_TITLE, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = SHARE_SUBTITLE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = SheetListMaxHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onShare,
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(containerColor = ipbGreen, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(SHARE_LABEL, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SaveConfirmationDialog(
    monthLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Salvar escala de $monthLabel?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Salvar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Those who lead the least come first; then by name. */
private fun membersByLeastUsed(members: List<Member>, counts: Map<Int, Int>, query: String): List<Member> {
    val q = query.normalize()
    return members.filter { it.name.normalize().contains(q, ignoreCase = true) }
        .sortedWith(compareBy({ counts[it.id] ?: 0 }, { it.name.normalize() }))
}

private fun initials(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.take(INITIALS).joinToString("") { it.first().uppercase() }

/** "TER", "QUI", "DOM". */
private fun EditableScheduleUiState.weekdayAbbreviation(): String =
    localDate()?.dayOfWeek?.getDisplayName(TextStyle.SHORT, PT_BR)
        ?.take(WEEKDAY_LETTERS)?.uppercase(PT_BR).orEmpty()

private fun EditableScheduleUiState.dayMonth(): String = localDate()?.format(DAY_MONTH) ?: DAY_FORMAT.format(day)

// ── Formatter helper ──────────────────────────────────────────────────────────

private fun AdminScheduleUiState.toWhatsappText(): String {
    val locale = java.util.Locale.forLanguageTag("pt-BR")
    val monthName = MonthScheduleWhatsappFormatter.monthPtBr(month).uppercase(locale)
    val order = listOf("terça", "terca", "quinta", "domingo")

    val grouped = items.groupBy { it.scheduleTypeName }
    val sortedGroups = grouped.entries.sortedWith(
        compareBy { entry ->
            val t = entry.key.trim().lowercase(locale)
            order.indexOfFirst { t.startsWith(it) }.let { if (it == -1) Int.MAX_VALUE else it }
        }
    )

    val sb = StringBuilder()
    sb.append("ESCALA DE $monthName $year - DIRIGENTES E RESPONSÁVEIS\n\n")

    sortedGroups.forEach { (typeName, groupItems) ->
        // time isn't stored in EditableScheduleUiState, use 19:30 as default (same as save)
        sb.append("$typeName (19:30)\n\n")
        groupItems.sortedBy { it.day }.forEach { item ->
            sb.append(String.format("%02d", item.day))
                .append("- ")
                .append(item.selectedMember?.name ?: "---")
                .append("\n")
        }
        sb.append("\n")
    }

    sb.append("*Cafezinho pós culto de Adoração (Ceia) todo 4° Domingo\n\n")
    sb.append("* Aberto a participação de qualquer irmão.\n\n")
    sb.append("DEUS ABENÇOE")

    return sb.toString().trim()
}
