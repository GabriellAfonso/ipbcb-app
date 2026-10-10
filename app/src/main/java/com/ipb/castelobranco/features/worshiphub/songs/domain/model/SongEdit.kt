package com.ipb.castelobranco.features.worshiphub.songs.domain.model

/** The editable fields of a catalogue song. An empty [youtubeLink] means "no link". */
data class SongEditFields(
    val title: String,
    val artist: String,
    val youtubeLink: String,
) {
    fun trimmed() = SongEditFields(title.trim(), artist.trim(), youtubeLink.trim())
}

/** One message per invalid field; null means the field is valid. */
data class SongFieldErrors(
    val title: String? = null,
    val artist: String? = null,
    val youtubeLink: String? = null,
) {
    val hasAny: Boolean get() = title != null || artist != null || youtubeLink != null
}

sealed interface SongWriteResult {
    data object Success : SongWriteResult
    data class Invalid(val errors: SongFieldErrors) : SongWriteResult
    data object Duplicate : SongWriteResult

    /** Delete refused because the song was played or is on a setlist; [message] is the API's pt-BR copy. */
    data class InUse(val message: String?) : SongWriteResult
    data object NotFound : SongWriteResult
    data object NoPermission : SongWriteResult
    data class Failed(val error: Throwable) : SongWriteResult
}
