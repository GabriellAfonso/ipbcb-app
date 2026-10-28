package com.ipb.castelobranco.features.gallery.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.gallery.domain.tags.MediaFilter
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryGridKeys
import com.ipb.castelobranco.features.gallery.presentation.components.PhotoTileImage
import com.ipb.castelobranco.features.gallery.presentation.navigation.GalleryNav
import com.ipb.castelobranco.features.gallery.presentation.state.PeopleUiState
import com.ipb.castelobranco.features.gallery.presentation.state.PersonRow
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoImage
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoTile
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.GalleryViewModel
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.PeopleViewModel
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.TagTexts

private const val GRID_COLUMNS = 3
private const val PEOPLE_TITLE = "Pessoas"
private const val SEARCH_LABEL = "Buscar pessoa"
private const val UNSELECT_LABEL = "Tirar do filtro"
private const val PERSON_KEY_PREFIX = "person-"
private const val BAND_KEY = "band"
private const val MEDIA_FILTER_KEY = "media-filter"
private const val RESULT_TITLE_KEY = "result-title"
private const val RESULT_EMPTY_KEY = "result-empty"
private const val PEOPLE_TITLE_KEY = "people-title"
private const val EMPTY_KEY = "empty"
private const val BOTTOM_KEY = "bottom"

private const val MUTED_ALPHA = 0.75f
private const val TEXT_ALPHA = 0.85f
private const val DIVIDER_ALPHA = 0.08f
private const val SHADOW_ALPHA = 0.12f

private val BandCorner = 28.dp
private val MediaOverlap = 18.dp
private val CardCorner = 18.dp
private val SidePadding = 16.dp
private val AvatarSize = 40.dp
private val TickSize = 24.dp

/** Fixed avatar colors, picked by member id: the same person always gets the same one. */
private val AvatarColors = listOf(
    Color(0xFF125B49), Color(0xFF1A9E8A), Color(0xFF504A7B), Color(0xFF9A5B00), Color(0xFF1565C0),
    Color(0xFF7B3F61), Color(0xFF2E6B2E), Color(0xFF8C3B3B), Color(0xFF3F4945),
)

/** The events of the people screen. */
data class PeopleActions(
    val onBack: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onToggle: (Long) -> Unit,
    val onPhotoClick: (memberIds: Set<Long>, filter: MediaFilter, photoId: Long) -> Unit,
    val onMediaFilter: (MediaFilter) -> Unit = {},
)

/**
 * The people filter. [galleryViewModel] (the graph's) only shows the gallery's
 * messages here — e.g. why the viewer opened from a result closed.
 */
@Composable
fun PeopleScreen(viewModel: PeopleViewModel, galleryViewModel: GalleryViewModel, nav: GalleryNav) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by galleryViewModel.message.collectAsStateWithLifecycle()

    GalleryMessageHost(message, isLeaving = false, galleryViewModel, nav) {
        PeopleContent(
            state = state,
            previewLoader = viewModel.previewLoader,
            actions = PeopleActions(
                onBack = nav.back,
                onQueryChange = viewModel::onQueryChange,
                onToggle = viewModel::toggle,
                onPhotoClick = nav.toPeoplePhoto,
                onMediaFilter = viewModel::onMediaFilter,
            ),
        )
    }
}

/** Band with the search and the chosen people, the result, then the people (`specs/gallery/spec.md` §10.6). */
@Composable
fun PeopleContent(state: PeopleUiState, previewLoader: ImageLoader?, actions: PeopleActions) {
    BaseScreen(
        tabName = PEOPLE_TITLE,
        logoRes = R.drawable.ic_galery,
        showBackArrow = true,
        onBackClick = actions.onBack,
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
                return@Box
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(GRID_COLUMNS),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = BAND_KEY, span = { GridItemSpan(maxLineSpan) }) {
                    PeopleBand(state, hasMediaFilter = state.showMediaFilter, actions)
                }
                if (state.showMediaFilter) {
                    item(key = MEDIA_FILTER_KEY, span = { GridItemSpan(maxLineSpan) }) {
                        MediaFilterSwitch(state.mediaFilter, actions.onMediaFilter, Modifier.offset(y = -MediaOverlap))
                    }
                }
                if (state.showResults) {
                    item(key = RESULT_TITLE_KEY, span = { GridItemSpan(maxLineSpan) }) {
                        SectionTitle(state.resultTitle.orEmpty(), detail = state.results.size.toString())
                    }
                    items(state.results, key = { GalleryGridKeys.photo(it.id) }) { photo ->
                        ResultTile(photo, previewLoader) {
                            actions.onPhotoClick(state.memberIds, state.mediaFilter, photo.id)
                        }
                    }
                    state.resultEmptyText?.let { text ->
                        item(key = RESULT_EMPTY_KEY, span = { GridItemSpan(maxLineSpan) }) { EmptyText(text) }
                    }
                }
                if (state.showSearch) {
                    item(key = PEOPLE_TITLE_KEY, span = { GridItemSpan(maxLineSpan) }) {
                        SectionTitle(TagTexts.PEOPLE_SECTION, detail = TagTexts.PEOPLE_SECTION_HINT)
                    }
                }
                itemsIndexed(
                    state.people,
                    key = { _, person -> PERSON_KEY_PREFIX + person.id },
                    span = { _, _ -> GridItemSpan(maxLineSpan) },
                ) { index, person ->
                    PersonItem(
                        person = person,
                        isFirst = index == 0,
                        isLast = index == state.people.lastIndex,
                        onClick = { actions.onToggle(person.id) },
                    )
                }
                state.emptyText?.let { text ->
                    item(key = EMPTY_KEY, span = { GridItemSpan(maxLineSpan) }) { EmptyText(text) }
                }
                item(key = BOTTOM_KEY, span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

/** "Quem aparece nas fotos", the counts, the search pill and the chosen people as white tags. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeopleBand(state: PeopleUiState, hasMediaFilter: Boolean, actions: PeopleActions) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = BandCorner, bottomEnd = BandCorner))
            .background(ipbGreen)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = if (hasMediaFilter) 34.dp else 20.dp),
    ) {
        if (state.showSearch) {
            Text(
                text = TagTexts.PEOPLE_BAND_TITLE,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = MUTED_ALPHA),
            )
            Text(
                text = state.peopleCountText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = state.taggedPhotosText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = TEXT_ALPHA),
            )
            Spacer(Modifier.height(10.dp))
            SearchPill(state.query, actions.onQueryChange)
        }
        if (state.selected.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.selected.forEach { person -> SelectedTag(person) { actions.onToggle(person.id) } }
            }
        }
    }
}

@Composable
private fun SearchPill(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = ipbGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(SEARCH_LABEL, style = MaterialTheme.typography.bodyMedium, color = SearchHint)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = SearchText),
                cursorBrush = SolidColor(ipbGreen),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val SearchHint = Color(0xFF3F4945)
private val SearchText = Color(0xFF191C1B)

@Composable
private fun SelectedTag(person: PersonRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White)
            .clickable(onClickLabel = UNSELECT_LABEL, onClick = onClick)
            .padding(start = 12.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = person.firstName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = ipbGreen,
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.Close, contentDescription = null, tint = ipbGreen, modifier = Modifier.size(16.dp))
    }
}

/** "Tudo / Fotos / Vídeos" over the band's edge. */
@Composable
private fun MediaFilterSwitch(selected: MediaFilter, onSelect: (MediaFilter) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(percent = 50), spotColor = Color.Black.copy(alpha = SHADOW_ALPHA))
                .clip(RoundedCornerShape(percent = 50))
                .background(highlightCardColor())
                .padding(4.dp),
        ) {
            MEDIA_FILTERS.forEach { (filter, label) ->
                val isSelected = filter == selected
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (isSelected) ipbGreen else Color.Transparent)
                        .clickable { onSelect(filter) }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ResultTile(photo: PhotoTile, previewLoader: ImageLoader?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        if (previewLoader != null) PhotoTileImage(photo, previewLoader, Modifier.fillMaxSize())
    }
}

/** A person's line of the card: rounded on top for the first, at the bottom for the last. */
@Composable
private fun PersonItem(person: PersonRow, isFirst: Boolean, isLast: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(
        topStart = if (isFirst) CardCorner else 0.dp,
        topEnd = if (isFirst) CardCorner else 0.dp,
        bottomStart = if (isLast) CardCorner else 0.dp,
        bottomEnd = if (isLast) CardCorner else 0.dp,
    )
    Column(
        modifier = Modifier
            .padding(horizontal = SidePadding)
            .fillMaxWidth()
            .clip(shape)
            .background(highlightCardColor()),
    ) {
        if (!isFirst) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = DIVIDER_ALPHA))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(AvatarSize)
                    .clip(CircleShape)
                    .background(AvatarColors[(person.id % AvatarColors.size).toInt()]),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = person.initials,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = person.countText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Tick(person.isChecked)
        }
    }
}

@Composable
private fun Tick(isChecked: Boolean) {
    val modifier = Modifier.size(TickSize).clip(CircleShape)
    if (isChecked) {
        Box(modifier.background(ipbGreen), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    } else {
        Box(modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
    )
}

private val MEDIA_FILTERS = listOf(
    MediaFilter.ALL to TagTexts.MEDIA_FILTER_ALL,
    MediaFilter.PHOTOS to TagTexts.MEDIA_FILTER_PHOTOS,
    MediaFilter.VIDEOS to TagTexts.MEDIA_FILTER_VIDEOS,
)

// region previews

private val previewActions = PeopleActions({}, {}, {}, { _, _, _ -> })

@Preview
@Composable
private fun PeopleListPreview() {
    PeopleContent(
        PeopleUiState(
            isLoading = false,
            showSearch = true,
            peopleCountText = "2 pessoas",
            taggedPhotosText = "4 fotos com alguém marcado",
            people = listOf(
                PersonRow(40, "João Lima (você)", "3 fotos", isChecked = false, firstName = "João", initials = "JL"),
                PersonRow(12, "Maria Souza", "1 foto", isChecked = false, firstName = "Maria", initials = "MS"),
            ),
        ),
        previewLoader = null,
        actions = previewActions,
    )
}

@Preview
@Composable
private fun PeopleResultPreview() {
    val selected = listOf(
        PersonRow(40, "João Lima", "3 fotos", true, firstName = "João", initials = "JL"),
        PersonRow(12, "Maria Souza", "1 foto", true, firstName = "Maria", initials = "MS"),
    )
    PeopleContent(
        PeopleUiState(
            isLoading = false,
            showSearch = true,
            peopleCountText = "2 pessoas",
            taggedPhotosText = "4 fotos com alguém marcado",
            people = selected,
            selected = selected,
            showResults = true,
            results = listOf(PhotoTile(1, PhotoImage.None)),
            showMediaFilter = true,
            resultTitle = "Fotos com João e Maria",
        ),
        previewLoader = null,
        actions = previewActions,
    )
}

// endregion
