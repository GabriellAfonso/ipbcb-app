package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.gallery.domain.model.GalleryAlbum
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GalleryTree
import com.ipb.castelobranco.features.gallery.domain.model.VideoDuration
import com.ipb.castelobranco.features.gallery.presentation.state.AlbumTile
import com.ipb.castelobranco.features.gallery.presentation.state.AlbumUiState
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoImage
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoTile
import com.ipb.castelobranco.features.gallery.presentation.state.ViewerPhoto

/** Pure mapping from the local copy to what the gallery screens show. */
internal object GalleryUiMapper {

    private const val HTTP_UNAUTHORIZED = 401
    private const val HTTP_FORBIDDEN = 403
    private const val DATE_PARTS = 3
    const val SESSION_EXPIRED_MESSAGE = "Sua sessão expirou. Entre novamente para acessar a galeria."
    const val NO_ACCESS_MESSAGE = "Disponível apenas para membros."
    const val NO_CONNECTION_MESSAGE = "Não foi possível carregar a galeria. Verifique sua conexão."

    /** [visible] the albums a member sees; `null` = all of them are (outside management modes). */
    fun albumTile(album: GalleryAlbum, local: GalleryLocalState, visible: Set<Long>? = null) = AlbumTile(
        id = album.id,
        name = album.name,
        cover = album.coverUrl?.let { local.covers[it] },
        onDevice = visible == null || album.id in visible,
        date = album.eventDate?.let(::formatDate),
    )

    fun photoImage(photo: GalleryPhoto, local: GalleryLocalState): PhotoImage =
        local.originals[photo.id]?.let { PhotoImage.Original(it) }
            ?: local.videos[photo.id]?.takeIf { photo.isVideo }?.let { PhotoImage.VideoFrame(it) }
            ?: photo.thumbnailUrl?.let { PhotoImage.Preview(it) }
            ?: PhotoImage.None

    fun photoTile(photo: GalleryPhoto, local: GalleryLocalState) = PhotoTile(
        id = photo.id,
        image = photoImage(photo, local),
        isVideo = photo.isVideo,
        duration = photo.durationMs?.takeIf { photo.isVideo }?.let(VideoDuration::format),
        onDevice = !photo.isVideo || photo.id in local.videos,
    )

    /**
     * [tree] is what the album shows: the visible tree, or the full one while "Organizar" is open there
     * — then [visibleAlbums] marks the sub-albums a member would not see.
     */
    fun albumState(
        albumId: Long,
        local: GalleryLocalState,
        tree: GalleryTree?,
        visibleAlbums: Set<Long>? = null,
    ): AlbumUiState {
        if (tree == null) return AlbumUiState(isLoading = true)
        val album = tree.album(albumId) ?: return AlbumUiState(isLoading = false, isRemoved = true)
        return AlbumUiState(
            isLoading = false,
            title = album.name,
            subtitle = tree.parentOf(albumId)?.name,
            eventDate = album.eventDate?.let(::formatDate),
            description = album.description.takeIf { it.isNotBlank() },
            subAlbums = tree.children(albumId).map { albumTile(it, local, visibleAlbums) },
            photos = tree.photosOf(albumId).map { photoTile(it, local) },
            hasOwnCover = album.coverUrl != null && album.coverSourceAlbumId == album.id,
            cover = album.coverUrl?.let { local.covers[it] },
        )
    }

    fun viewerPhoto(photo: GalleryPhoto, local: GalleryLocalState) = ViewerPhoto(
        id = photo.id,
        title = photo.name.substringBeforeLast('.'),
        fileName = photo.name,
        image = photoImage(photo, local),
        albumId = photo.albumId,
        description = photo.description.takeIf { it.isNotBlank() },
        dateTaken = photo.dateTaken?.let(::formatDate),
        people = photo.members.map { it.name },
        isVideo = photo.isVideo,
        videoFile = local.videos[photo.id]?.takeIf { photo.isVideo },
        mimeType = photo.mimeType(),
    )

    /** `yyyy-MM-dd` → `dd/MM/yyyy`; anything else is shown as it came. */
    fun formatDate(raw: String): String {
        val parts = raw.take(10).split('-')
        return if (parts.size == DATE_PARTS) "${parts[2]}/${parts[1]}/${parts[0]}" else raw
    }

    fun errorCode(error: AppError): Int? = when (error) {
        is AppError.Auth -> error.code
        is AppError.Server -> error.code
        else -> null
    }

    fun syncErrorMessage(error: AppError): String = when {
        error is AppError.Auth && error.code == HTTP_UNAUTHORIZED -> SESSION_EXPIRED_MESSAGE
        error is AppError.Auth && error.code == HTTP_FORBIDDEN -> error.userMessage ?: NO_ACCESS_MESSAGE
        error is AppError.Network -> NO_CONNECTION_MESSAGE
        else -> error.toUserMessage()
    }

    fun isForbidden(error: AppError?): Boolean = error is AppError.Auth && error.code == HTTP_FORBIDDEN
}
