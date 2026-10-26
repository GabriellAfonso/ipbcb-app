package com.ipb.castelobranco.features.admin.events.domain.usecase

import com.ipb.castelobranco.features.admin.events.domain.model.EventDraft
import com.ipb.castelobranco.features.admin.events.domain.model.EventField
import com.ipb.castelobranco.features.admin.events.domain.repository.ImageBoundsReader
import javax.inject.Inject

/**
 * The rules the server would refuse anyway, checked before sending so an impossible event never becomes
 * a request (`specs/events/spec.md` §4.2).
 *
 * @param hasImage an image is picked now, or the event already has one.
 */
class ValidateEventDraftUseCase @Inject constructor() {
    operator fun invoke(draft: EventDraft, hasImage: Boolean): Map<EventField, String> = buildMap {
        val title = draft.title.trim()
        when {
            title.isEmpty() -> put(EventField.TITLE, "Informe o título.")
            title.length > TITLE_MAX -> put(EventField.TITLE, "O título pode ter até $TITLE_MAX caracteres.")
        }
        if (draft.startDate == null) put(EventField.START_DATE, "Informe a data de início.")
        if (draft.startDate != null && draft.endDate != null && draft.endDate.isBefore(draft.startDate)) {
            put(EventField.END_DATE, "A data de fim não pode ser antes da data de início.")
        }
        if (draft.location.trim().length > LOCATION_MAX) {
            put(EventField.LOCATION, "O local pode ter até $LOCATION_MAX caracteres.")
        }
        if (!hasImage) put(EventField.IMAGE, "Escolha uma imagem.")
    }

    companion object {
        const val TITLE_MAX = 120
        const val LOCATION_MAX = 200
    }
}

/** The server refuses flyers under 600×400; the crop never upscales, so a small source is caught here. */
class ValidateEventImageUseCase @Inject constructor(
    private val boundsReader: ImageBoundsReader,
) {
    /** @return the message for the image field, or null when the image is usable. */
    operator fun invoke(bytes: ByteArray): String? {
        val bounds = boundsReader.read(bytes) ?: return UNREADABLE
        return if (bounds.width < MIN_WIDTH || bounds.height < MIN_HEIGHT) TOO_SMALL else null
    }

    companion object {
        const val MIN_WIDTH = 600
        const val MIN_HEIGHT = 400
        const val TOO_SMALL = "A imagem precisa ter pelo menos 600×400 pixels."
        const val UNREADABLE = "Não foi possível ler esta imagem."
    }
}
