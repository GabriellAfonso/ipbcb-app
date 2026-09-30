package com.ipb.castelobranco.features.gallery.presentation.state

import com.ipb.castelobranco.features.gallery.domain.model.TreeTarget

/** The one dialog or sheet open over a gallery screen. */
sealed interface GalleryDialogState {
    data class Form(val form: ItemFormState) : GalleryDialogState
    data class MovePicker(val picker: MovePickerState) : GalleryDialogState
    data class Confirm(val confirm: ConfirmState) : GalleryDialogState
}

/** What a form edits: a new album (under [parentId], `null` = root), an album, or a photo. */
sealed interface FormTarget {
    data class NewAlbum(val parentId: Long?) : FormTarget
    data class Album(val albumId: Long) : FormTarget
    data class Photo(val photoId: Long) : FormTarget
}

/** Name, description and a date (event date or date taken), as the user types them. */
data class ItemFormState(
    val target: FormTarget,
    val name: String = "",
    val description: String = "",
    /** `yyyy-MM-dd`, or `null` for none. */
    val date: String? = null,
    val nameError: String? = null,
    val isSaving: Boolean = false,
) {
    val title: String
        get() = when (target) {
            is FormTarget.NewAlbum -> "Novo álbum"
            is FormTarget.Album -> "Editar álbum"
            is FormTarget.Photo -> "Editar foto"
        }

    val dateLabel: String get() = if (target is FormTarget.Photo) "Data da foto" else "Data do evento"
}

/** What is being moved. [fromViewer]: a single photo moved from the viewer. */
sealed interface MoveSubject {
    data class Album(val albumId: Long) : MoveSubject
    data class Photos(val albumId: Long, val photoIds: List<Long>, val fromViewer: Boolean) : MoveSubject
}

data class MovePickerState(
    val subject: MoveSubject,
    val targets: List<TreeTarget>,
    val isSaving: Boolean = false,
) {
    val title: String
        get() = when (subject) {
            is MoveSubject.Album -> "Mover álbum para"
            is MoveSubject.Photos -> if (subject.photoIds.size == 1) "Mover foto para" else "Mover fotos para"
        }
}

sealed interface ConfirmAction {
    data class DeleteAlbum(val albumId: Long) : ConfirmAction
    data class DeletePhotos(val albumId: Long, val photoIds: List<Long>, val fromViewer: Boolean) : ConfirmAction
    data class RemoveCover(val albumId: Long) : ConfirmAction
}

data class ConfirmState(
    val action: ConfirmAction,
    val text: String,
    val confirmLabel: String,
    val isRunning: Boolean = false,
)
