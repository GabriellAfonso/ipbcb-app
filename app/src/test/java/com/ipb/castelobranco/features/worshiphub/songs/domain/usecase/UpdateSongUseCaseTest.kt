package com.ipb.castelobranco.features.worshiphub.songs.domain.usecase

import com.ipb.castelobranco.features.worshiphub.songs.domain.FakeSongEditRepository
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongEditFields
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongWriteResult
import com.ipb.castelobranco.features.worshiphub.songs.domain.validation.SongEditValidator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSongUseCaseTest {

    private val repository = FakeSongEditRepository()
    private val useCase = UpdateSongUseCase(repository)
    private val original = SongEditFields("Ocenas", "Hillsong", "")

    @Test
    fun `sends the trimmed fields when something changed`() = runTest {
        val result = useCase(7, original, SongEditFields(" Oceans ", "Hillsong", " https://youtu.be/x "))

        assertEquals(SongWriteResult.Success, result)
        assertEquals(7 to SongEditFields("Oceans", "Hillsong", "https://youtu.be/x"), repository.updates.single())
    }

    @Test
    fun `invalid fields are not sent`() = runTest {
        val result = useCase(7, original, original.copy(title = ""))

        assertEquals(SongEditValidator.TITLE_REQUIRED, (result as SongWriteResult.Invalid).errors.title)
        assertTrue(repository.updates.isEmpty())
    }

    @Test
    fun `nothing changed besides surrounding spaces is a success without a call`() = runTest {
        val result = useCase(7, original, original.copy(artist = " Hillsong  "))

        assertEquals(SongWriteResult.Success, result)
        assertTrue(repository.updates.isEmpty())
    }

    @Test
    fun `repository failure is returned as is`() = runTest {
        repository.updateResult = SongWriteResult.Duplicate

        assertEquals(SongWriteResult.Duplicate, useCase(7, original, original.copy(title = "Oceans")))
    }
}
