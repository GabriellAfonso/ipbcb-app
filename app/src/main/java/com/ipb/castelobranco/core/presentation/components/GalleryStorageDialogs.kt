package com.ipb.castelobranco.core.presentation.components

import com.ipb.castelobranco.core.domain.gallery.GalleryMissing
import com.ipb.castelobranco.core.domain.gallery.GalleryUsage

/** What the person checked in "Apagar galeria" / "Baixar galeria". */
data class GalleryStorageChoice(
    val photos: Boolean,
    val videos: Boolean,
    val useMobileData: Boolean = false,
)

/**
 * The two gallery storage dialogs, shared by Configurações › Galeria and the gallery itself (its
 * "Baixar galeria" after a delete). Videos stay opt-in: "Baixar galeria" never pre-checks them.
 */
object GalleryStorageDialogs {
    const val DELETE_TITLE = "Apagar galeria"
    const val DOWNLOAD_TITLE = "Baixar galeria"
    const val PHOTOS = "Fotos"
    const val VIDEOS = "Vídeos"
    const val PHOTOS_DONE = "Já baixadas"
    const val VIDEOS_AUTO = "Já são baixados automaticamente"
    const val NO_VIDEOS = "Nenhum vídeo para baixar"
    const val DELETE_MESSAGE = "Escolha o que apagar deste aparelho."
    const val DELETE_NOTE_VIDEOS = "Apagar os vídeos desliga o download automático de vídeos."
    const val DELETE_NOTE_ALL = "A galeria só volta a ser baixada quando você tocar em \"Baixar galeria\"."
    const val DOWNLOAD_MESSAGE = "Escolha o que baixar para este aparelho."
    const val DOWNLOAD_NOTE_VIDEOS = "Vídeos novos também serão baixados. Somente no WiFi."
    const val MOBILE_DATA = "Usar dados móveis para as fotos"
    const val DELETE_LABEL = "Apagar"
    const val DOWNLOAD_LABEL = "Baixar"
    const val DELETED_ALL = "Galeria apagada"
    const val DELETED_PHOTOS = "Fotos apagadas"
    const val DELETED_VIDEOS = "Vídeos apagados"

    /** Both types pre-checked. */
    fun initialDelete(): GalleryStorageChoice = GalleryStorageChoice(photos = true, videos = true)

    fun deleteDialog(usage: GalleryUsage?, choice: GalleryStorageChoice) = MediaTypesDialogState(
        title = DELETE_TITLE,
        message = DELETE_MESSAGE,
        options = listOf(
            MediaTypeOption(PHOTOS, usage?.photoBytes?.let(::formatDiskUsage), checked = choice.photos),
            MediaTypeOption(VIDEOS, usage?.videoBytes?.let(::formatDiskUsage), checked = choice.videos),
        ),
        confirmLabel = DELETE_LABEL,
        note = when {
            choice.photos && choice.videos -> DELETE_NOTE_ALL
            choice.videos -> DELETE_NOTE_VIDEOS
            else -> null
        },
    )

    fun deletedMessage(choice: GalleryStorageChoice): String = when {
        choice.photos && choice.videos -> DELETED_ALL
        choice.videos -> DELETED_VIDEOS
        else -> DELETED_PHOTOS
    }

    /** Photos pre-checked when some are missing; videos never (opt-in). */
    fun initialDownload(missing: GalleryMissing): GalleryStorageChoice =
        GalleryStorageChoice(photos = photosAvailable(missing), videos = false)

    fun downloadDialog(missing: GalleryMissing, autoDownloadVideos: Boolean, choice: GalleryStorageChoice) =
        MediaTypesDialogState(
            title = DOWNLOAD_TITLE,
            message = DOWNLOAD_MESSAGE,
            options = listOf(
                MediaTypeOption(
                    PHOTOS,
                    detail = PHOTOS_DONE.takeUnless { photosAvailable(missing) },
                    checked = choice.photos && photosAvailable(missing),
                    enabled = photosAvailable(missing),
                ),
                MediaTypeOption(
                    VIDEOS,
                    detail = when {
                        autoDownloadVideos -> VIDEOS_AUTO
                        !videosAvailable(missing) -> NO_VIDEOS
                        else -> null
                    },
                    checked = choice.videos && videosAvailable(missing) && !autoDownloadVideos,
                    enabled = videosAvailable(missing) && !autoDownloadVideos,
                ),
            ),
            confirmLabel = DOWNLOAD_LABEL,
            note = DOWNLOAD_NOTE_VIDEOS.takeIf { choice.videos && !autoDownloadVideos },
            extra = MediaTypeOption(
                MOBILE_DATA,
                checked = choice.useMobileData && choice.photos,
                enabled = choice.photos && photosAvailable(missing),
            ),
        )

    /** After a full delete nothing is known yet: both types may come back. */
    private fun photosAvailable(missing: GalleryMissing) = missing.isPaused || missing.photos > 0

    private fun videosAvailable(missing: GalleryMissing) = missing.isPaused || missing.videos > 0
}
