package com.ipb.castelobranco.features.gallery.presentation.state

import com.ipb.castelobranco.features.gallery.domain.model.GalleryMember
import com.ipb.castelobranco.features.gallery.domain.model.TreeTarget
import com.ipb.castelobranco.features.gallery.domain.tags.NameSearch

/** The one dialog or sheet open over a gallery screen. */
sealed interface GalleryDialogState {
    data class Form(val form: ItemFormState) : GalleryDialogState
    data class MovePicker(val picker: MovePickerState) : GalleryDialogState
    data class Confirm(val confirm: ConfirmState) : GalleryDialogState
    data class PeoplePicker(val picker: PeoplePickerState) : GalleryDialogState
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

/** Whose people the picker edits: one photo (the full set), or a selection (people to add or remove). */
sealed interface PickerMode {
    data class Photo(val photoId: Long) : PickerMode
    data class Add(val albumId: Long, val photoIds: List<Long>) : PickerMode
    data class Remove(val albumId: Long, val photoIds: List<Long>) : PickerMode
}

/**
 * The people picker. [people] is the list to choose from, in display order: for a photo its current
 * people first, then everyone else by name; for "Remover pessoas" only the people of the selection.
 * Never stored — read again every time it opens.
 */
data class PeoplePickerState(
    val mode: PickerMode,
    val people: List<GalleryMember> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val checked: Set<Long> = emptySet(),
    val isSaving: Boolean = false,
) {
    val visiblePeople: List<GalleryMember> get() = people.filter { NameSearch.matches(it.name, query) }

    val title: String
        get() = when (mode) {
            is PickerMode.Photo -> "Marcar pessoas"
            is PickerMode.Add -> "Adicionar pessoas"
            is PickerMode.Remove -> "Remover pessoas"
        }

    val confirmLabel: String
        get() = when (mode) {
            is PickerMode.Photo -> "Salvar"
            is PickerMode.Add -> "Adicionar"
            is PickerMode.Remove -> "Remover"
        }

    /** A photo may be saved with nobody (clears it); a selection needs someone to add or remove. */
    val canConfirm: Boolean
        get() = !isSaving && !isLoading && error == null && (mode is PickerMode.Photo || checked.isNotEmpty())

    /** The text in place of the list, when it is empty. */
    val emptyText: String?
        get() = when {
            isLoading || error != null -> null
            people.isEmpty() -> "Nenhuma pessoa cadastrada."
            visiblePeople.isEmpty() -> "Nenhuma pessoa encontrada."
            else -> null
        }
}
