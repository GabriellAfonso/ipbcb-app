package com.ipb.castelobranco.features.worshiphub.songs.domain.usecase

import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongEditFields
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongWriteResult
import com.ipb.castelobranco.features.worshiphub.songs.domain.repository.SongEditRepository
import com.ipb.castelobranco.features.worshiphub.songs.domain.validation.SongEditValidator
import javax.inject.Inject

class UpdateSongUseCase @Inject constructor(
    private val repository: SongEditRepository,
) {
    /** Validates [edited] and sends it only when it differs from [original]. */
    suspend operator fun invoke(id: Int, original: SongEditFields, edited: SongEditFields): SongWriteResult {
        val errors = SongEditValidator.validate(edited)
        if (errors.hasAny) return SongWriteResult.Invalid(errors)
        val trimmed = edited.trimmed()
        if (trimmed == original.trimmed()) return SongWriteResult.Success
        return repository.update(id, trimmed)
    }
}

class DeleteSongUseCase @Inject constructor(
    private val repository: SongEditRepository,
) {
    suspend operator fun invoke(id: Int): SongWriteResult = repository.delete(id)
}
