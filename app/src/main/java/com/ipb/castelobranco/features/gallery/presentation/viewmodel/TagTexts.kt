package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.domain.manage.isNotFound
import com.ipb.castelobranco.features.gallery.domain.tags.TagBatchResult

/** Portuguese texts of the member tags. Pure, so they are tested as is. */
internal object TagTexts {

    const val SAVED = "Marcações salvas"
    const val NOT_FOUND = "Algumas fotos ou pessoas não existem mais. Confira e tente de novo."
    const val NO_RESULT = "Nenhuma foto com todas essas pessoas."
    const val NO_TAGS = "Ninguém foi marcado nas fotos ainda."
    const val NO_PERSON_FOUND = "Nenhuma pessoa encontrada."
    const val NO_PHOTOS = "Nenhuma foto."
    const val NO_VIDEOS = "Nenhum vídeo."
    const val MEDIA_FILTER_ALL = "Tudo"
    const val MEDIA_FILTER_PHOTOS = "Fotos"
    const val MEDIA_FILTER_VIDEOS = "Vídeos"
    const val PEOPLE_BAND_TITLE = "Quem aparece nas fotos"
    const val PEOPLE_SECTION = "Pessoas"
    const val PEOPLE_SECTION_HINT = "toque para filtrar"
    const val ALL_PHOTOS = "Todas as fotos"
    const val ALL_VIDEOS = "Todos os vídeos"

    /** The user's own row in the filter: "João Lima (você)". */
    fun ownName(name: String): String = "$name (você)"

    /** "1 foto", "3 fotos". */
    fun photoCount(count: Int): String = if (count == 1) "1 foto" else "$count fotos"

    /** "1 pessoa", "9 pessoas". */
    fun peopleCount(count: Int): String = if (count == 1) "1 pessoa" else "$count pessoas"

    /** "214 fotos com alguém marcado". */
    fun taggedPhotos(count: Int): String = "${photoCount(count)} com alguém marcado"

    /**
     * The result's title: "Fotos de Ana", "Fotos com Ana e João", "Fotos com Ana, João e Bruno"; with nobody
     * chosen, the media filter's: "Todas as fotos" / "Todos os vídeos".
     */
    fun resultTitle(firstNames: List<String>, videosOnly: Boolean): String = when (firstNames.size) {
        0 -> if (videosOnly) ALL_VIDEOS else ALL_PHOTOS
        1 -> "Fotos de ${firstNames.single()}"
        else -> "Fotos com ${firstNames.dropLast(1).joinToString(", ")} e ${firstNames.last()}"
    }

    /** "Ana Clara Rocha" → "AC". */
    fun initials(name: String): String =
        name.split(' ').filter { it.isNotBlank() }.take(INITIALS).joinToString("") { it.first().uppercase() }

    /** "Ana Clara Rocha" → "Ana". */
    fun firstName(name: String): String = name.trim().substringBefore(' ')

    private const val INITIALS = 2

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
