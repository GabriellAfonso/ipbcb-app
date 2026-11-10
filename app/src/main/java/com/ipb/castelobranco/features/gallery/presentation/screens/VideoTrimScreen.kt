package com.ipb.castelobranco.features.gallery.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.gallery.presentation.components.TrimTimeline
import com.ipb.castelobranco.features.gallery.presentation.components.TrimVideoPlayer
import com.ipb.castelobranco.features.gallery.presentation.navigation.GalleryNav
import com.ipb.castelobranco.features.gallery.presentation.state.TrimHandle
import com.ipb.castelobranco.features.gallery.presentation.state.TrimTimeEditor
import com.ipb.castelobranco.features.gallery.presentation.state.VideoTrimUiState
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.GalleryViewModel
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.VideoTexts

/** Everything the editor can ask for; each is a plain event, decided in the controller. */
data class VideoTrimActions(
    val onBack: () -> Unit,
    val onRangeChange: (startMs: Long, endMs: Long) -> Unit,
    val onMoveSelection: (startMs: Long) -> Unit,
    val onConfirm: () -> Unit,
    val onCancelCut: () -> Unit,
    val onSendAllWhole: () -> Unit,
    val onKeepEditing: () -> Unit,
    val onDiscard: () -> Unit,
    val onEditTime: (TrimHandle) -> Unit,
    val onTypeTime: (String) -> Unit,
    val onApplyTime: () -> Unit,
    val onCloseTimeEditor: () -> Unit,
)

@Composable
fun VideoTrimScreen(viewModel: GalleryViewModel, nav: GalleryNav) {
    val controller = viewModel.videoTrim
    val state by controller.state.collectAsStateWithLifecycle()

    // Sem lote (decidido, descartado ou o processo morreu): volta ao álbum.
    val isGone = state == null
    LaunchedEffect(isGone) {
        if (isGone) nav.back()
    }
    val current = state ?: return

    BackHandler(onBack = controller::askDiscard)
    VideoTrimContent(
        state = current,
        previewLoader = viewModel.previewLoader,
        actions = VideoTrimActions(
            onBack = controller::askDiscard,
            onRangeChange = controller::setRange,
            onMoveSelection = controller::moveSelection,
            onConfirm = controller::confirm,
            onCancelCut = controller::cancelCut,
            onSendAllWhole = controller::sendAllWhole,
            onKeepEditing = controller::keepEditing,
            onDiscard = controller::discard,
            onEditTime = controller::editTime,
            onTypeTime = controller::typeTime,
            onApplyTime = controller::applyTime,
            onCloseTimeEditor = controller::closeTimeEditor,
        ),
    )
}

@Composable
fun VideoTrimContent(
    state: VideoTrimUiState,
    previewLoader: ImageLoader,
    actions: VideoTrimActions,
) {
    BaseScreen(
        tabName = VideoTexts.TRIM_TITLE,
        logoRes = R.drawable.ic_galery,
        showBackArrow = true,
        onBackClick = actions.onBack,
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.positionLabel?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
            TrimVideoPlayer(
                source = state.source,
                videoKey = state.position,
                startMs = state.startMs,
                endMs = state.endMs,
                previewMs = state.previewMs,
                isCutting = state.isCutting,
                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            )
            TrimTimeline(
                source = state.source,
                durationMs = state.durationMs,
                startMs = state.startMs,
                endMs = state.endMs,
                enabled = state.canMoveHandles,
                previewLoader = previewLoader,
                onRangeChange = actions.onRangeChange,
                onMoveSelection = actions.onMoveSelection,
            )
            TrimLabels(state, actions.onEditTime)
            (state.error ?: state.tooBigMessage)?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            if (state.isCutting) CutProgress(state, actions) else TrimButtons(state, actions)
        }
    }
    if (state.askingDiscard) {
        DiscardDialog(actions)
    } else {
        state.timeEditor?.let { TimeDialog(it, actions) }
    }
}

/** Start and end are tappable to type them; the selection sits between them. */
@Composable
private fun TrimLabels(state: VideoTrimUiState, onEditTime: (TrimHandle) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeChip(state.startLabel, state.canMoveHandles) { onEditTime(TrimHandle.START) }
        Text(state.selectionLabel, style = MaterialTheme.typography.labelLarge)
        TimeChip(state.endLabel, state.canMoveHandles) { onEditTime(TrimHandle.END) }
    }
}

@Composable
private fun TimeChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClickLabel = VideoTexts.EDIT_TIME, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        if (enabled) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun TimeDialog(editor: TrimTimeEditor, actions: VideoTrimActions) {
    val focus = remember { FocusRequester() }
    var field by remember(editor.handle) {
        mutableStateOf(TextFieldValue(editor.text, selection = TextRange(0, editor.text.length)))
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = actions.onCloseTimeEditor,
        title = { Text(editor.title) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = {
                    field = it
                    actions.onTypeTime(it.text)
                },
                singleLine = true,
                placeholder = { Text(VideoTexts.TIME_HINT) },
                supportingText = { Text(editor.error ?: VideoTexts.TIME_HINT) },
                isError = editor.error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { actions.onApplyTime() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = actions.onApplyTime) { Text(VideoTexts.APPLY) } },
        dismissButton = { TextButton(onClick = actions.onCloseTimeEditor) { Text(VideoTexts.CANCEL) } },
    )
}

@Composable
private fun CutProgress(state: VideoTrimUiState, actions: VideoTrimActions) {
    Text(VideoTexts.cutting(state.progress), style = MaterialTheme.typography.bodyMedium)
    LinearProgressIndicator(
        progress = { state.progress / PERCENT },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(onClick = actions.onCancelCut, modifier = Modifier.fillMaxWidth()) {
        Text(VideoTexts.CANCEL)
    }
}

@Composable
private fun TrimButtons(state: VideoTrimUiState, actions: VideoTrimActions) {
    Button(
        onClick = actions.onConfirm,
        enabled = state.primaryEnabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(state.primaryLabel)
    }
    if (state.showSendAllWhole) {
        OutlinedButton(onClick = actions.onSendAllWhole, modifier = Modifier.fillMaxWidth()) {
            Text(VideoTexts.SEND_ALL_WHOLE)
        }
    }
}

@Composable
private fun DiscardDialog(actions: VideoTrimActions) {
    AlertDialog(
        onDismissRequest = actions.onKeepEditing,
        title = { Text(VideoTexts.DISCARD_QUESTION) },
        text = { Text(VideoTexts.DISCARD_DETAIL) },
        confirmButton = { TextButton(onClick = actions.onDiscard) { Text(VideoTexts.DISCARD) } },
        dismissButton = { TextButton(onClick = actions.onKeepEditing) { Text(VideoTexts.KEEP_EDITING) } },
    )
}

private const val PERCENT = 100f
