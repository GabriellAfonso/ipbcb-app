package com.ipb.castelobranco.features.admin.events.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.events.domain.model.EventDraft
import com.ipb.castelobranco.features.admin.events.domain.model.EventField
import com.ipb.castelobranco.features.admin.events.domain.model.toDraft
import com.ipb.castelobranco.features.admin.events.testing.FakeEventsAdminRepository
import com.ipb.castelobranco.features.admin.events.testing.record
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EventWriteUseCasesTest {

    private val repository = FakeEventsAdminRepository()
    private val save = SaveEventUseCase(repository)
    private val image = byteArrayOf(1, 2, 3)

    @Test
    fun `without an original it creates with the image`() = runTest {
        val draft = EventDraft(title = "Almoço", startDate = LocalDate.of(2026, 12, 6))

        val saved = save(draft, image, original = null, originalDraft = null).getOrThrow()

        assertEquals(FakeEventsAdminRepository.NEW_ID, saved.id)
        val (changes, sentImage) = repository.creates.single()
        assertEquals("Almoço", changes.values[EventField.TITLE])
        assertSame(image, sentImage)
    }

    @Test
    fun `an edit patches only the changes and keeps the image when none was picked`() = runTest {
        val original = record(id = 4)

        save(original.toDraft().copy(location = ""), null, original, original.toDraft()).getOrThrow()

        val (id, changes, sentImage) = repository.updates.single()
        assertEquals(4, id)
        assertEquals(mapOf(EventField.LOCATION to ""), changes.values)
        assertEquals(null, sentImage)
    }

    @Test
    fun `an edit with nothing changed and no image sends nothing`() = runTest {
        val original = record(id = 4)

        val saved = save(original.toDraft(), null, original, original.toDraft()).getOrThrow()

        assertSame(original, saved)
        assertTrue(repository.updates.isEmpty())
    }

    @Test
    fun `a refused write is passed through`() = runTest {
        repository.writeResult = Result.failure(AppError.Server(code = 400))

        val result = save(EventDraft(title = "X", startDate = LocalDate.of(2026, 1, 1)), image, null, null)

        assertTrue(result.exceptionOrNull() is AppError.Server)
    }

    @Test
    fun `delete passes success and failure through`() = runTest {
        val delete = DeleteEventUseCase(repository)
        assertTrue(delete(3).isSuccess)

        repository.deleteResult = Result.failure(AppError.Server(code = 404))
        assertTrue(delete(3).isFailure)
        assertEquals(listOf(3, 3), repository.deletes)
    }
}
