package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.domain.manage.isNotFound
import com.ipb.castelobranco.features.gallery.domain.tags.TagBatchResult

/** Portuguese texts of the member tags. Pure, so they are tested as is. */
internal object TagTexts {

    const val SAVED = "Marcações salvas"
    const val NOT_FOUND = "Algumas fotos ou pessoas não existem mais. Confira e tente de novo."
    const val RESULT_HINT = "Fotos com todas as pessoas selecionadas"
    const val NO_RESULT = "Nenhuma foto com todas essas pessoas."
    const val NO_TAGS = "Ninguém foi marcado nas fotos ainda."
    const val NO_PERSON_FOUND = "Nenhuma pessoa encontrada."

    /** The user's own row in the filter: "João Lima (você)". */
    fun ownName(name: String): String = "$name (você)"

    /** "1 foto", "3 fotos". */
    fun photoCount(count: Int): String = if (count == 1) "1 foto" else "$count fotos"

    /**
     * "Marcações atualizadas em 3 fotos" when every request went through; otherwise
     * "Marcações atualizadas em 200 de 250 fotos: <motivo>", or just the reason when none did.
     */
    fun updated(result: TagBatchResult): String {
        val failure = result.failure ?: return "Marcações atualizadas em ${photoCount(result.updated)}"
        val reason = failure(failure)
        if (result.updated == 0) return reason
        return "Marcações atualizadas em ${result.updated} de ${result.total} fotos: $reason"
    }

    /** A tag write's refusal: a photo or a person gone meanwhile, or the usual write texts. */
    fun failure(error: AppError): String =
        if (error.isNotFound()) NOT_FOUND else error.toGalleryWriteError().message
}
