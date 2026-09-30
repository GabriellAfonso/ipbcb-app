package com.ipb.castelobranco.features.gallery.domain.tags

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.domain.manage.FakeGalleryManageRepository
import com.ipb.castelobranco.features.gallery.domain.model.GalleryMember
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagUseCasesTest {

    private val repository = FakeGalleryManageRepository()
    private val setMembers = SetPhotoMembersUseCase(repository)
    private val changeMembers = ChangePhotoMembersUseCase(repository)

    @Test
    fun `loading taggable members passes the list through`() = runTest {
        repository.taggableResult = Result.success(listOf(GalleryMember(1, "Ana")))

        assertEquals(listOf(GalleryMember(1, "Ana")), LoadTaggableMembersUseCase(repository)().getOrThrow())
        assertEquals(1, repository.taggableReads)
    }

    @Test
    fun `single save sends the full set`() = runTest {
        val result = setMembers(photoId = 5, current = listOf(12, 40), next = setOf(40, 7))

        assertEquals(TagSaveResult.Saved, result)
        assertEquals(listOf("setMembers:5:[7, 40]"), repository.calls)
    }

    @Test
    fun `single save sends nothing when the set did not change`() = runTest {
        val result = setMembers(photoId = 5, current = listOf(40, 12), next = setOf(12, 40))

        assertEquals(TagSaveResult.Unchanged, result)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `single save failure carries the error`() = runTest {
        val notFound = AppError.Server(code = 404)
        repository.tagFailures += notFound

        val result = setMembers(photoId = 5, current = emptyList(), next = setOf(12))

        assertEquals(TagSaveResult.Failed(notFound), result)
    }

    @Test
    fun `bulk add is split in requests of 200 and syncs once`() = runTest {
        val result = changeMembers((1L..450L).toList(), listOf(12), remove = false)

        assertEquals(listOf(200, 200, 50), repository.memberChanges.map { it.first.size })
        assertTrue(repository.memberChanges.all { it.second == listOf(12L) && it.third.isEmpty() })
        assertEquals(TagBatchResult(updated = 450, total = 450, failure = null), result)
        assertEquals(1, repository.syncs)
    }

    @Test
    fun `bulk remove sends the ids as removals`() = runTest {
        changeMembers(listOf(1, 2), listOf(12, 40), remove = true)

        assertEquals(Triple(listOf(1L, 2L), emptyList<Long>(), listOf(12L, 40L)), repository.memberChanges.single())
    }

    @Test
    fun `a failing request does not stop the others - partial count and the first error`() = runTest {
        val notFound = AppError.Server(code = 404)
        repository.tagFailures += listOf(null, notFound, null)

        val result = changeMembers((1L..450L).toList(), listOf(12), remove = false)

        assertEquals(3, repository.memberChanges.size)
        assertEquals(TagBatchResult(updated = 250, total = 450, failure = notFound), result)
        assertEquals(1, repository.syncs)
    }

    @Test
    fun `lost access stops the batch, and nothing syncs when nothing went through`() = runTest {
        repository.tagFailures += AppError.Auth(code = 403)

        val result = changeMembers((1L..450L).toList(), listOf(12), remove = false)

        assertEquals(1, repository.memberChanges.size)
        assertEquals(0, result.updated)
        assertEquals(0, repository.syncs)
    }
}
