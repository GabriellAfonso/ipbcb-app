package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ipb.castelobranco.core.di.DefaultDispatcher
import com.ipb.castelobranco.core.domain.member.ObserveOwnMemberIdUseCase
import com.ipb.castelobranco.features.gallery.di.GalleryThumbnailLoader
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GalleryTree
import com.ipb.castelobranco.features.gallery.domain.model.GalleryVisibility
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.tags.GalleryPeople
import com.ipb.castelobranco.features.gallery.domain.tags.MediaFilter
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
 * The people filter, read from the gallery's local copy only (offline, and always in step with the photos
 * on the device). The user's own member, when linked, comes first. Lives with its screen entry: the
 * selection survives a trip to the viewer and is gone once the screen is left.
 */
@HiltViewModel
class PeopleViewModel @Inject constructor(
    repository: GalleryRepository,
    observeOwnMemberId: ObserveOwnMemberIdUseCase,
    @param:GalleryThumbnailLoader val previewLoader: ImageLoader,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private data class Local(val local: GalleryLocalState, val tree: GalleryTree?, val people: List<TaggedPerson>)

    private val query = MutableStateFlow("")
    private val selection = MutableStateFlow<Set<Long>>(emptySet())
    private val mediaFilter = MutableStateFlow(MediaFilter.ALL)

    /** Everyone tagged in the last computed state: a toggle drops people no longer tagged anywhere. */
    @Volatile
    private var taggedIds: Set<Long> = emptySet()

    // What members see: undownloaded videos are not in the filter either.
    private val local = repository.localState.map { local ->
        val tree = local.index?.let { GalleryTree(GalleryVisibility.visibleIndex(it, local.videos.keys)) }
        Local(local, tree, tree?.let(GalleryPeople::taggedPeople).orEmpty())
    }

    val state: StateFlow<PeopleUiState> = combine(
        local,
        query,
        combine(selection, mediaFilter, ::Pair),
        observeOwnMemberId(),
    ) { local, query, (selection, mediaFilter), ownMemberId ->
        filter(local, query, selection, mediaFilter, ownMemberId)
    }.flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PeopleUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onMediaFilter(filter: MediaFilter) {
        mediaFilter.value = filter
        query.value = ""
    }

    /** Checks or unchecks [memberId]. Picking from the list clears the search, so the results show. */
    fun toggle(memberId: Long) {
        selection.update { selected ->
            val current = selected.filterTo(LinkedHashSet()) { it in taggedIds }
            if (memberId in current) current - memberId else current + memberId
        }
        query.value = ""
    }

    private fun filter(
        local: Local,
        query: String,
        selection: Set<Long>,
        requestedFilter: MediaFilter,
        ownMemberId: Long?,
    ): PeopleUiState {
        val tree = local.tree ?: return PeopleUiState(isLoading = true)
        taggedIds = local.people.mapTo(HashSet()) { it.id }
        val showMediaFilter = tree.allPhotosInTreeOrder().any { it.isVideo }
        // Without any video on the device the chips are gone, and so is their effect.
        val mediaFilter = if (showMediaFilter) requestedFilter else MediaFilter.ALL
        if (local.people.isEmpty() && !showMediaFilter) {
            return PeopleUiState(isLoading = false, emptyText = TagTexts.NO_TAGS)
        }
        // A person no longer tagged anywhere leaves the list and the selection.
        val selectedIds = selection.filterTo(LinkedHashSet()) { it in taggedIds }
        // The user first; sortedBy is stable, so everyone else keeps the name order.
        val people = local.people.sortedBy { it.id != ownMemberId }
        val selected = people.filter { it.id in selectedIds }.map { it.toRow(checked = true, ownMemberId) }
        val matching = people.filter { NameSearch.matches(it.name, query) }
            .map { it.toRow(checked = it.id in selectedIds, ownMemberId) }
        val showResults = (selectedIds.isNotEmpty() || mediaFilter != MediaFilter.ALL) && query.isBlank()
        val results = if (showResults) tiles(tree, local.local, selectedIds, mediaFilter) else emptyList()
        val taggedPhotos = tree.allPhotosInTreeOrder().count { it.members.isNotEmpty() }
        return PeopleUiState(
            isLoading = false,
            query = query,
            showSearch = true,
            peopleCountText = TagTexts.peopleCount(local.people.size),
            taggedPhotosText = TagTexts.taggedPhotos(taggedPhotos),
            people = matching,
            selected = selected,
            showResults = showResults,
            results = results,
            memberIds = selectedIds,
            showMediaFilter = showMediaFilter,
            mediaFilter = mediaFilter,
            resultTitle = if (showResults) {
                TagTexts.resultTitle(selected.map { it.firstName }, videosOnly = mediaFilter == MediaFilter.VIDEOS)
            } else {
                null
            },
            resultEmptyText = when {
                !showResults || results.isNotEmpty() -> null
                selectedIds.isNotEmpty() -> TagTexts.NO_RESULT
                mediaFilter == MediaFilter.VIDEOS -> TagTexts.NO_VIDEOS
                else -> TagTexts.NO_PHOTOS
            },
            emptyText = when {
                local.people.isEmpty() -> TagTexts.NO_TAGS
                matching.isEmpty() -> TagTexts.NO_PERSON_FOUND
                else -> null
            },
        )
    }

    private fun tiles(
        tree: GalleryTree,
        local: GalleryLocalState,
        memberIds: Set<Long>,
        filter: MediaFilter,
    ): List<PhotoTile> = GalleryPeople.matching(tree, memberIds, filter).map { GalleryUiMapper.photoTile(it, local) }

    private fun TaggedPerson.toRow(checked: Boolean, ownMemberId: Long?) = PersonRow(
        id = id,
        name = if (id == ownMemberId) TagTexts.ownName(name) else name,
        countText = TagTexts.photoCount(photoCount),
        isChecked = checked,
        firstName = TagTexts.firstName(name),
        initials = TagTexts.initials(name),
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
