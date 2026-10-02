package com.ipb.castelobranco.features.worshiphub.tables.domain.usecase

import com.ipb.castelobranco.core.testing.FakeWallClock
import com.ipb.castelobranco.features.worshiphub.tables.domain.FakeRepertoireDraftRepository
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.DraftRow
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.RepertoireDraft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepertoireDraftUseCasesTest {

    private val rows = listOf(
        DraftRow(position = 1, songId = 12, tone = "G", isFixed = true),
        DraftRow(position = 2, songId = 55, tone = "A#", isFixed = false),
        DraftRow(position = 3, songId = null, tone = "", isFixed = false),
        DraftRow(position = 4, songId = null, tone = "", isFixed = false),
    )
    private val savedAt = 10_000_000L

    @Test
    fun `a draft changed less than an hour ago comes back as saved`() = runTest {
        val repository = FakeRepertoireDraftRepository(RepertoireDraft(rows, savedAt))
        val clock = FakeWallClock(savedAt + 59 * 60_000)

        assertEquals(rows, RestoreRepertoireDraftUseCase(repository, clock)(setOf(12, 55)))
    }

    @Test
    fun `an expired draft is discarded`() = runTest {
        val repository = FakeRepertoireDraftRepository(RepertoireDraft(rows, savedAt))
        val clock = FakeWallClock(savedAt + DraftExpiry.DRAFT_TTL_MS)

        assertNull(RestoreRepertoireDraftUseCase(repository, clock)(setOf(12, 55)))
        assertNull(repository.stored)
    }

    @Test
    fun `a song no longer in the catalog comes back as an empty row`() = runTest {
        val repository = FakeRepertoireDraftRepository(RepertoireDraft(rows, savedAt))

        val restored = RestoreRepertoireDraftUseCase(repository, FakeWallClock(savedAt))(setOf(55))!!

        assertEquals(DraftRow(1, null, "", false), restored[0])
        assertEquals(rows[1], restored[1])
    }

    @Test
    fun `no draft restores nothing`() = runTest {
        assertNull(RestoreRepertoireDraftUseCase(FakeRepertoireDraftRepository(), FakeWallClock())(emptySet()))
    }

    @Test
    fun `saving stamps the current time, renewing the hour`() = runTest {
        val repository = FakeRepertoireDraftRepository()
        val clock = FakeWallClock(savedAt)

        SaveRepertoireDraftUseCase(repository, clock)(rows)
        clock.now = savedAt + 50 * 60_000
        SaveRepertoireDraftUseCase(repository, clock)(rows)

        assertEquals(savedAt + 50 * 60_000, repository.stored?.updatedAtMillis)
    }

    @Test
    fun `clearing removes the draft`() = runTest {
        val repository = FakeRepertoireDraftRepository(RepertoireDraft(rows, savedAt))

        ClearRepertoireDraftUseCase(repository)()

        assertNull(repository.stored)
    }
}
