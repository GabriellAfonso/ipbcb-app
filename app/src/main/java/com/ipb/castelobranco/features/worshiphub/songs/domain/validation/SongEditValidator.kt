package com.ipb.castelobranco.features.worshiphub.songs.domain.validation

import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongEditFields
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongFieldErrors
import java.net.URI

/** Same rules as `PATCH api/songs/{id}/`, so the user sees the error on the field before anything is sent. */
object SongEditValidator {

    const val MAX_TEXT_LENGTH = 100
    const val MAX_LINK_LENGTH = 200

    const val TITLE_REQUIRED = "Informe o nome."
    const val ARTIST_REQUIRED = "Informe o artista."
    const val TEXT_TOO_LONG = "Use até $MAX_TEXT_LENGTH caracteres."
    const val LINK_INVALID = "Link inválido. Use um endereço http(s)."
    const val LINK_TOO_LONG = "Use até $MAX_LINK_LENGTH caracteres."

    /** For a field the API rejected without the app catching it first. */
    const val TITLE_INVALID = "Nome inválido."
    const val ARTIST_INVALID = "Artista inválido."

    private val ALLOWED_SCHEMES = setOf("http", "https")

    fun validate(fields: SongEditFields): SongFieldErrors {
        val trimmed = fields.trimmed()
        return SongFieldErrors(
            title = validateText(trimmed.title, TITLE_REQUIRED),
            artist = validateText(trimmed.artist, ARTIST_REQUIRED),
            youtubeLink = validateLink(trimmed.youtubeLink),
        )
    }

    private fun validateText(value: String, requiredMessage: String): String? = when {
        value.isEmpty() -> requiredMessage
        value.length > MAX_TEXT_LENGTH -> TEXT_TOO_LONG
        else -> null
    }

    private fun validateLink(value: String): String? = when {
        value.isEmpty() -> null
        value.length > MAX_LINK_LENGTH -> LINK_TOO_LONG
        !isHttpUrl(value) -> LINK_INVALID
        else -> null
    }

    private fun isHttpUrl(value: String): Boolean {
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        return uri.scheme?.lowercase() in ALLOWED_SCHEMES && !uri.host.isNullOrBlank()
    }
}
