package com.ipb.castelobranco.features.gallery.presentation.state

import com.ipb.castelobranco.features.gallery.domain.trash.TrashKey
import java.io.File

/** What a photo tile or page shows: the original on disk, else the server preview, else grey. */
sealed interface PhotoImage {
    data class Original(val file: File) : PhotoImage
    data class Preview(val url: String) : PhotoImage
    data object None : PhotoImage
}

/** An album in a grid. [cover] `null` = black tile. */
data class AlbumTile(val id: Long, val name: String, val cover: File?)

data class PhotoTile(val id: Long, val image: PhotoImage)

data class GalleryDownloadState(
    val isDownloading: Boolean = false,
    val isPending: Boolean = false,
    /** Waiting out a backoff after a rate limit or a network failure — not an error. */
    val isResuming: Boolean = false,
    val downloaded: Int = 0,
    val total: Int = 0,
    val error: String? = null,
    val errorCode: Int? = null,
    val isResolved: Boolean = false,
)

data class GalleryRootUiState(
    /** No index yet and the first sync has not answered. */
    val isLoading: Boolean = true,
    val hasIndex: Boolean = false,
    val albums: List<AlbumTile> = emptyList(),
    val download: GalleryDownloadState = GalleryDownloadState(),
    val isOnWifi: Boolean = false,
    /** Why the gallery could not be loaded — only set when there is no index to show. */
    val syncError: String? = null,
    val syncErrorCode: Int? = null,
    /** The feed answered 403 while the device still holds the gallery: shown above the grid. */
    val showMembersOnlyNotice: Boolean = false,
    val permissions: GalleryPermissions = GalleryPermissions.NONE,
    /** "Organizar" open on the root: [albums] already follow the draft order. */
    val isOrganizing: Boolean = false,
    val isSavingOrder: Boolean = false,
) {
    /** The trash icon: owners only, not while organizing. */
    val showTrash: Boolean get() = permissions.canDelete && !isOrganizing

    /** The people filter: everyone, once the gallery is on the device, not while organizing. */
    val showPeople: Boolean get() = hasIndex && !isOrganizing
}

data class AlbumUiState(
    val isLoading: Boolean = true,
    /** The album left the index (deleted on the server): the screen goes up one level. */
    val isRemoved: Boolean = false,
    val title: String = "",
    val subtitle: String? = null,
    val eventDate: String? = null,
    val description: String? = null,
    val subAlbums: List<AlbumTile> = emptyList(),
    val photos: List<PhotoTile> = emptyList(),
    val permissions: GalleryPermissions = GalleryPermissions.NONE,
    /** The album's cover is its own (not inherited, not absent): an owner may remove it. */
    val hasOwnCover: Boolean = false,
    /** "Organizar" open here: [subAlbums] and [photos] already follow the draft order. */
    val isOrganizing: Boolean = false,
    val isSavingOrder: Boolean = false,
    /** Photos picked in selection mode; empty = not selecting. */
    val selection: Set<Long> = emptySet(),
    val uploads: AlbumUploadsUiState = AlbumUploadsUiState(),
    /** Someone is tagged in a selected photo: "Remover pessoas" has something to remove. */
    val canRemovePeople: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && !isRemoved && subAlbums.isEmpty() && photos.isEmpty()
    val isSelecting: Boolean get() = selection.isNotEmpty()
    val canRemoveCover: Boolean get() = permissions.canDelete && hasOwnCover
}

/** The upload queue as seen from one album. */
data class AlbumUploadsUiState(
    /** Picked photos still being copied into the app. */
    val isCopying: Boolean = false,
    /** Photos of this album still to be sent. */
    val pending: Int = 0,
    /** The queue's progress (all albums): the photo being sent and the batch size; 0 when idle. */
    val current: Int = 0,
    val total: Int = 0,
    val failed: List<FailedUpload> = emptyList(),
) {
    val isVisible: Boolean get() = isCopying || pending > 0 || failed.isNotEmpty()
}

data class FailedUpload(val uploadId: String, val name: String, val reason: String)

data class ViewerPhoto(
    val id: Long,
    /** Photo name without extension, for the top bar. */
    val title: String,
    /** File name used when saving to the device. */
    val fileName: String,
    val image: PhotoImage,
    /** The album the photo is in — not always the one the viewer pages through. */
    val albumId: Long = 0L,
    /** `null` when empty. */
    val description: String? = null,
    /** `dd/MM/yyyy`, `null` when unknown. */
    val dateTaken: String? = null,
    /** The names of the people tagged, in the photo's order. */
    val people: List<String> = emptyList(),
) {
    val original: File? get() = (image as? PhotoImage.Original)?.file
    val canSaveOrShare: Boolean get() = original != null
}

/** What a viewer pages through: the album it was opened from, or a people filter's result. */
sealed interface ViewerSource {
    data class Album(val albumId: Long) : ViewerSource

    /** The photos in which every one of [memberIds] is tagged, in tree order. */
    data class People(val memberIds: Set<Long>) : ViewerSource
}

data class PhotoViewerUiState(
    val isLoading: Boolean = true,
    val photos: List<ViewerPhoto> = emptyList(),
    /** The page to show; moves when the photo on screen leaves the album. */
    val currentIndex: Int = 0,
    /** No photo left in the album: the viewer closes. */
    val isClosed: Boolean = false,
    val permissions: GalleryPermissions = GalleryPermissions.NONE,
)

/**
 * A one-shot notice, shown once by whichever gallery screen is on top: an item that left the gallery
 * while it was on screen, or the outcome of a management action.
 */
sealed interface GalleryMessage {
    val text: String

    /** A button on the message, if any. */
    val action: MessageAction? get() = null

    data object PhotoRemoved : GalleryMessage {
        override val text = "Esta foto foi removida"
    }

    data object PhotoMoved : GalleryMessage {
        override val text = "Esta foto foi movida para outro álbum"
    }

    /** The photo on screen no longer has every person of the filter it was opened from. */
    data object PhotoLeftResult : GalleryMessage {
        override val text = "Esta foto não está mais no resultado"
    }

    data object AlbumRemoved : GalleryMessage {
        override val text = "Este álbum foi removido"
    }

    data object AlbumCreated : GalleryMessage {
        override val text = "Álbum criado"
    }

    data object Saved : GalleryMessage {
        override val text = "Alterações salvas"
    }

    /** [undo] is offered only after deleting this one album, while the user still has `owner`. */
    data class AlbumTrashed(val undo: MessageAction.Undo? = null) : GalleryMessage {
        override val text = "Álbum enviado para a lixeira"
        override val action: MessageAction? get() = undo
    }

    /** [undo] is offered only after deleting this one photo, while the user still has `owner`. */
    data class PhotoTrashed(val undo: MessageAction.Undo? = null) : GalleryMessage {
        override val text = "Foto enviada para a lixeira"
        override val action: MessageAction? get() = undo
    }

    data class PhotoMovedTo(val albumName: String) : GalleryMessage {
        override val text = "Foto movida para '$albumName'"
    }

    data object OrderChanged : GalleryMessage {
        override val text = "A ordem mudou enquanto você editava. Confira e salve de novo."
    }

    data object CoverUpdated : GalleryMessage {
        override val text = "Capa atualizada"
    }

    data object CoverRemoved : GalleryMessage {
        override val text = "Capa removida"
    }

    /** A batch result or a refusal, already worded; [action] e.g. "Abrir álbum" after a name conflict. */
    data class Text(override val text: String, override val action: MessageAction? = null) : GalleryMessage
}

/** What a message's button does. */
sealed interface MessageAction {
    val label: String

    /** Restores what was just sent to the trash. */
    data class Undo(val key: TrashKey) : MessageAction {
        override val label = "Desfazer"
    }

    /** Opens the live album that holds the name a restore needs. */
    data class OpenAlbum(val albumId: Long) : MessageAction {
        override val label = "Abrir álbum"
    }
}
