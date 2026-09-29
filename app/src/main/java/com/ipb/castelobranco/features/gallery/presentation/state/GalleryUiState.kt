package com.ipb.castelobranco.features.gallery.presentation.state

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
)

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
) {
    val isEmpty: Boolean get() = !isLoading && !isRemoved && subAlbums.isEmpty() && photos.isEmpty()
}

data class ViewerPhoto(
    val id: Long,
    /** Photo name without extension, for the top bar. */
    val title: String,
    /** File name used when saving to the device. */
    val fileName: String,
    val image: PhotoImage,
) {
    val original: File? get() = (image as? PhotoImage.Original)?.file
    val canSaveOrShare: Boolean get() = original != null
}

data class PhotoViewerUiState(
    val isLoading: Boolean = true,
    val photos: List<ViewerPhoto> = emptyList(),
    /** The page to show; moves when the photo on screen leaves the album. */
    val currentIndex: Int = 0,
    /** No photo left in the album: the viewer closes. */
    val isClosed: Boolean = false,
)

/** A one-shot notice about an item that left the gallery while it was on screen. */
enum class GalleryMessage(val text: String) {
    PhotoRemoved("Esta foto foi removida"),
    PhotoMoved("Esta foto foi movida para outro álbum"),
    AlbumRemoved("Este álbum foi removido"),
}
