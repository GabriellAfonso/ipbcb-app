package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.core.domain.member.ObserveOwnMemberIdUseCase
import com.ipb.castelobranco.core.testing.FakeCurrentMemberRepository
import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GalleryMember
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.presentation.state.PeopleUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val localState = MutableStateFlow(GalleryLocalState.EMPTY)
    private val member = FakeCurrentMemberRepository()

    private val ana = GalleryMember(1, "Ana")
    private val joao = GalleryMember(2, "João")
    private val bruno = GalleryMember(3, "Bruno")

    private fun photo(id: Long, albumId: Long, position: Int, vararg people: GalleryMember): GalleryPhoto =
        galleryPhoto(id, albumId = albumId, position = position).copy(members = people.toList())

    /** Album 1 (root) holds 10 (Ana), 11 (Ana, João); album 2 (root, after 1) holds 20 (Ana, João, Bruno). */
    private val photos = listOf(
        photo(10, albumId = 1, position = 0, ana),
        photo(11, albumId = 1, position = 1, ana, joao),
        photo(20, albumId = 2, position = 0, joao, bruno, ana),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        publish(photos)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun publish(photos: List<GalleryPhoto>) {
        val albums = listOf(galleryAlbum(1), galleryAlbum(2, position = 1))
        localState.value = GalleryLocalState(
            GalleryIndex(albums.associateBy { it.id }, photos.associateBy { it.id }, "c"),
            emptyMap(),
            emptyMap(),
        )
    }

    private fun viewModel(): PeopleViewModel {
        val repository = mockk<GalleryRepository> { every { localState } returns this@PeopleViewModelTest.localState }
        return PeopleViewModel(
            repository = repository,
            observeOwnMemberId = ObserveOwnMemberIdUseCase(member),
            previewLoader = mockk(relaxed = true),
            defaultDispatcher = dispatcher,
        )
    }

    private fun TestScope.observe(viewModel: PeopleViewModel): () -> PeopleUiState {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        return { advanceUntilIdle(); viewModel.state.value }
    }

    @Test
    fun `lists every tagged person with counts, by name, and no results yet`() = runTest(dispatcher) {
        val state = observe(viewModel())

        val people = state().people
        assertEquals(listOf("Ana", "Bruno", "João"), people.map { it.name })
        assertEquals(listOf("3 fotos", "1 foto", "2 fotos"), people.map { it.countText })
        assertFalse(state().showResults)
        assertTrue(state().showSearch)
    }

    @Test
    fun `search ignores accents and case`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val state = observe(viewModel)

        viewModel.onQueryChange("JOAO")

        assertEquals(listOf("João"), state().people.map { it.name })
    }

    @Test
    fun `search without a match says so`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val state = observe(viewModel)

        viewModel.onQueryChange("pedro")

        assertTrue(state().people.isEmpty())
        assertEquals(TagTexts.NO_PERSON_FOUND, state().emptyText)
    }

    @Test
    fun `selecting people shows the AND result in tree order, with the hint from two people`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)

            viewModel.toggle(1)
            assertEquals(listOf(10L, 11L, 20L), state().results.map { it.id })
            assertNull(state().hint)

            viewModel.toggle(2)
            assertEquals(listOf(11L, 20L), state().results.map { it.id })
            assertEquals(TagTexts.RESULT_HINT, state().hint)
            assertEquals(setOf(1L, 2L), state().memberIds)

            viewModel.toggle(3)
            assertEquals(listOf(20L), state().results.map { it.id })
        }

    @Test
    fun `no photo with all of them - empty text`() = runTest(dispatcher) {
        publish(photos + photo(30, albumId = 2, position = 1, GalleryMember(4, "Davi")))
        val viewModel = viewModel()
        val state = observe(viewModel)

        viewModel.toggle(3)
        viewModel.toggle(4)

        assertTrue(state().results.isEmpty())
        assertEquals(TagTexts.NO_RESULT, state().emptyText)
    }

    @Test
    fun `picking from a search clears it so the results show`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val state = observe(viewModel)
        viewModel.onQueryChange("bru")
        state()

        viewModel.toggle(3)

        assertEquals("", state().query)
        assertTrue(state().showResults)
        assertEquals(listOf("Bruno"), state().selected.map { it.name })
    }

    @Test
    fun `the result follows the local copy, and a person tagged nowhere leaves the selection`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val state = observe(viewModel)
            viewModel.toggle(3)
            assertEquals(listOf(20L), state().results.map { it.id })

            publish(listOf(photos[0], photos[1], photo(20, albumId = 2, position = 0, joao, ana)))

            assertTrue(state().selected.isEmpty())
            assertFalse(state().showResults)
            assertEquals(listOf("Ana", "João"), state().people.map { it.name })
        }

    @Test
    fun `nobody tagged anywhere`() = runTest(dispatcher) {
        publish(listOf(photo(10, albumId = 1, position = 0)))

        val state = observe(viewModel())

        assertEquals(TagTexts.NO_TAGS, state().emptyText)
        assertFalse(state().showSearch)
    }

    @Test
    fun `the user comes first, marked, and follows the profile live`() = runTest(dispatcher) {
        member.state.value = 3
        val viewModel = viewModel()
        val state = observe(viewModel)

        assertEquals(listOf("Bruno (você)", "Ana", "João"), state().people.map { it.name })

        member.state.value = 2
        assertEquals(listOf("João (você)", "Ana", "Bruno"), state().people.map { it.name })

        member.state.value = null
        assertEquals(listOf("Ana", "Bruno", "João"), state().people.map { it.name })
    }

    @Test
    fun `picking the user shows their photos, and the search matches only the name`() = runTest(dispatcher) {
        member.state.value = 3
        val viewModel = viewModel()
        val state = observe(viewModel)

        viewModel.onQueryChange("você")
        assertTrue(state().people.isEmpty())

        viewModel.toggle(3)
        assertEquals(listOf(20L), state().results.map { it.id })
        assertEquals(listOf("Bruno (você)"), state().selected.map { it.name })
    }

    @Test
    fun `a user tagged nowhere is not listed`() = runTest(dispatcher) {
        member.state.value = 4

        val state = observe(viewModel())

        assertEquals(listOf("Ana", "Bruno", "João"), state().people.map { it.name })
    }
}
