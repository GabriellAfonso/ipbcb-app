package com.ipb.castelobranco.features.gallery.presentation.state

import com.ipb.castelobranco.features.gallery.domain.trash.TrashKey

/** A trash entry as the screen shows it, every text already worded. */
data class TrashRow(
    val key: TrashKey,
    val title: String,
    val kindLabel: String,
    val deletedLine: String,
    /** Albums only: what went with it; `null` when nothing did. */
    val countsLine: String?,
    /** Photos only, when the uploader is known. */
    val uploadedLine: String?,
    val purgeLine: String,
    val thumbnailUrl: String?,
)

data class TrashUiState(
    /** The first read has not answered yet. */
    val isLoading: Boolean = true,
    /** A reload (pull, or after an item left the trash) with rows already on screen. */
    val isRefreshing: Boolean = false,
    /** The last read failed and there is nothing to show. */
    val error: String? = null,
    val rows: List<TrashRow> = emptyList(),
    /** The row whose restore is running; one at a time. */
    val restoring: TrashKey? = null,
    /** The trashed parent album to restore first. */
    val highlighted: TrashKey? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && rows.isEmpty()
    val canRefresh: Boolean get() = restoring == null
}

sealed interface TrashEvent {
    /** [openAlbumId]: the live album that holds the name, offered as "Abrir álbum". */
    data class Message(val text: String, val openAlbumId: Long? = null) : TrashEvent

    /** The user lost `owner`: the screen closes saying why. */
    data class Close(val text: String) : TrashEvent
}
