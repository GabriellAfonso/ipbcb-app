package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ipb.castelobranco.core.di.DefaultDispatcher
import com.ipb.castelobranco.core.domain.member.ObserveOwnMemberIdUseCase
import com.ipb.castelobranco.features.gallery.di.GalleryThumbnailLoader
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GalleryTree
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.tags.GalleryPeople
import com.ipb.castelobranco.features.gallery.domain.tags.NameSearch
import com.ipb.castelobranco.features.gallery.domain.tags.TaggedPerson
import com.ipb.castelobranco.features.gallery.presentation.state.PeopleUiState
import com.ipb.castelobranco.features.gallery.presentation.state.PersonRow
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoTile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * The people filter and "Minhas fotos", read from the gallery's local copy only (offline, and always in
 * step with the photos on the device). Lives with its screen entry: the selection survives a trip to the
 * viewer and is gone once the screen is left.
 */
@HiltViewModel
class PeopleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: GalleryRepository,
    observeOwnMemberId: ObserveOwnMemberIdUseCase,
    @param:GalleryThumbnailLoader val previewLoader: ImageLoader,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val isMine: Boolean = savedStateHandle.get<Boolean>(ARG_MINE) ?: false

    private data class Local(val local: GalleryLocalState, val tree: GalleryTree?, val people: List<TaggedPerson>)

    private val query = MutableStateFlow("")
    private val selection = MutableStateFlow<Set<Long>>(emptySet())

    /** "Minhas fotos" closes only after a link was seen: the first emission may come before the profile. */
    @Volatile
    private var sawMember = false

    /** Everyone tagged in the last computed state: a toggle drops people no longer tagged anywhere. */
    @Volatile
    private var taggedIds: Set<Long> = emptySet()

    private val local = repository.localState.map { local ->
        val tree = local.index?.let(::GalleryTree)
        Local(local, tree, tree?.let(GalleryPeople::taggedPeople).orEmpty())
    }

    val state: StateFlow<PeopleUiState> = combine(local, query, selection, observeOwnMemberId()) {
            local, query, selection, ownMemberId ->
        if (isMine) mine(local, ownMemberId) else filter(local, query, selection)
    }.flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PeopleUiState(isMine = isMine))

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Checks or unchecks [memberId]. Picking from the list clears the search, so the results show. */
    fun toggle(memberId: Long) {
        selection.update { selected ->
            val current = selected.filterTo(LinkedHashSet()) { it in taggedIds }
            if (memberId in current) current - memberId else current + memberId
        }
        query.value = ""
    }

    private fun filter(local: Local, query: String, selection: Set<Long>): PeopleUiState {
        val tree = local.tree ?: return PeopleUiState(isLoading = true)
        taggedIds = local.people.mapTo(HashSet()) { it.id }
        if (local.people.isEmpty()) return PeopleUiState(isLoading = false, emptyText = TagTexts.NO_TAGS)
        // A person no longer tagged anywhere leaves the list and the selection.
        val selectedIds = selection.filterTo(LinkedHashSet()) { it in taggedIds }
        val selected = local.people.filter { it.id in selectedIds }.map { it.toRow(checked = true) }
        val matching = local.people.filter { NameSearch.matches(it.name, query) }
            .map { it.toRow(checked = it.id in selectedIds) }
        val showResults = selectedIds.isNotEmpty() && query.isBlank()
        val results = if (showResults) tiles(tree, local.local, selectedIds) else emptyList()
        return PeopleUiState(
            isLoading = false,
            query = query,
            showSearch = true,
            people = matching,
            selected = selected,
            showResults = showResults,
            results = results,
            memberIds = selectedIds,
            hint = TagTexts.RESULT_HINT.takeIf { showResults && selectedIds.size > 1 },
            emptyText = when {
                showResults && results.isEmpty() -> TagTexts.NO_RESULT
                !showResults && matching.isEmpty() -> TagTexts.NO_PERSON_FOUND
                else -> null
            },
        )
    }

    private fun mine(local: Local, ownMemberId: Long?): PeopleUiState {
        if (ownMemberId == null) {
            return PeopleUiState(isLoading = !sawMember, isMine = true, isClosed = sawMember)
        }
        sawMember = true
        val tree = local.tree ?: return PeopleUiState(isLoading = true, isMine = true)
        val memberIds = setOf(ownMemberId)
        val results = tiles(tree, local.local, memberIds)
        return PeopleUiState(
            isLoading = false,
            isMine = true,
            showResults = true,
            results = results,
            memberIds = memberIds,
            emptyText = TagTexts.MY_PHOTOS_EMPTY.takeIf { results.isEmpty() },
        )
    }

    private fun tiles(tree: GalleryTree, local: GalleryLocalState, memberIds: Set<Long>): List<PhotoTile> =
        GalleryPeople.photosWithAll(tree, memberIds).map { PhotoTile(it.id, GalleryUiMapper.photoImage(it, local)) }

    private fun TaggedPerson.toRow(checked: Boolean) = PersonRow(id, name, TagTexts.photoCount(photoCount), checked)

    companion object {
        /** Route argument: `true` opens "Minhas fotos". */
        const val ARG_MINE = "mine"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
