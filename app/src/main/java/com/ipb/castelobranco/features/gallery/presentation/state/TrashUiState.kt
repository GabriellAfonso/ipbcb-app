package com.ipb.castelobranco.features.gallery.presentation.state

import com.ipb.castelobranco.features.gallery.domain.trash.TrashKey

/** A trash entry as the screen shows it, every text already worded. */
data class TrashRow(
    val key: TrashKey,
    val title: String,
    /** "Álbum · 2 subálbuns · 41 fotos" or "Foto". */
    val kindLine: String,
    /** "Apagado por Maria em 29/09 · some em 29/10". */
    val detailLine: String,
    /** Gone within a week: listed under "Somem nesta semana". */
    val isPurgeSoon: Boolean,
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
    val soonRows: List<TrashRow> get() = rows.filter { it.isPurgeSoon }
    val laterRows: List<TrashRow> get() = rows.filterNot { it.isPurgeSoon }
    val canRefresh: Boolean get() = restoring == null
}

sealed interface TrashEvent {
    /** [openAlbumId]: the live album that holds the name, offered as "Abrir álbum". */
    data class Message(val text: String, val openAlbumId: Long? = null) : TrashEvent

    /** The user lost `owner`: the screen closes saying why. */
    data class Close(val text: String) : TrashEvent
}
