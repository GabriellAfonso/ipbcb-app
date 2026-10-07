package com.ipb.castelobranco.features.gallery.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryGridKeys
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryImage
import com.ipb.castelobranco.features.gallery.presentation.components.PersonCheckRow
import com.ipb.castelobranco.features.gallery.presentation.navigation.GalleryNav
import com.ipb.castelobranco.features.gallery.presentation.state.PeopleUiState
import com.ipb.castelobranco.features.gallery.presentation.state.PersonRow
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoImage
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoTile
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.GalleryViewModel
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.PeopleViewModel

private const val GRID_COLUMNS = 3
private const val PEOPLE_TITLE = "Pessoas"
private const val SEARCH_LABEL = "Buscar pessoa"
private const val UNSELECT_LABEL = "Tirar do filtro"
private const val PERSON_KEY_PREFIX = "person-"
private const val SEARCH_KEY = "search"
private const val CHIPS_KEY = "chips"
private const val HINT_KEY = "hint"
private const val EMPTY_KEY = "empty"

/** The events of the people screen. */
data class PeopleActions(
    val onBack: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onToggle: (Long) -> Unit,
    val onPhotoClick: (memberIds: Set<Long>, photoId: Long) -> Unit,
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
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.showSearch) {
                    item(key = SEARCH_KEY, span = { GridItemSpan(maxLineSpan) }) {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = actions.onQueryChange,
                            label = { Text(SEARCH_LABEL) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (state.selected.isNotEmpty()) {
                    item(key = CHIPS_KEY, span = { GridItemSpan(maxLineSpan) }) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.selected.forEach { person ->
                                InputChip(
                                    selected = true,
                                    onClick = { actions.onToggle(person.id) },
                                    label = { Text(person.name) },
                                    trailingIcon = {
                                        Icon(Icons.Filled.Close, contentDescription = UNSELECT_LABEL)
                                    },
                                )
                            }
                        }
                    }
                }
                state.hint?.let { hint ->
                    item(key = HINT_KEY, span = { GridItemSpan(maxLineSpan) }) {
                        Text(hint, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (state.showResults) {
                    items(state.results, key = { GalleryGridKeys.photo(it.id) }) { photo ->
                        ResultTile(photo, previewLoader) { actions.onPhotoClick(state.memberIds, photo.id) }
                    }
                } else {
                    items(
                        state.people,
                        key = { PERSON_KEY_PREFIX + it.id },
                        span = { GridItemSpan(maxLineSpan) },
                    ) { person ->
                        PersonCheckRow(
                            name = person.name,
                            detail = person.countText,
                            checked = person.isChecked,
                            enabled = true,
                            onClick = { actions.onToggle(person.id) },
                        )
                    }
                }
                state.emptyText?.let { text ->
                    item(key = EMPTY_KEY, span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultTile(photo: PhotoTile, previewLoader: ImageLoader?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
    ) {
        if (previewLoader != null) {
            GalleryImage(
                image = photo.image,
                previewLoader = previewLoader,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// region previews

private val previewActions = PeopleActions({}, {}, {}, { _, _ -> })

@Preview
@Composable
private fun PeopleListPreview() {
    PeopleContent(
        PeopleUiState(
            isLoading = false,
            showSearch = true,
            people = listOf(
                PersonRow(40, "João Lima (você)", "3 fotos", isChecked = false),
                PersonRow(12, "Maria Souza", "1 foto", isChecked = false),
            ),
        ),
        previewLoader = null,
        actions = previewActions,
    )
}

@Preview
@Composable
private fun PeopleEmptyResultPreview() {
    val selected = listOf(PersonRow(40, "João Lima", "3 fotos", true), PersonRow(12, "Maria Souza", "1 foto", true))
    PeopleContent(
        PeopleUiState(
            isLoading = false,
            showSearch = true,
            selected = selected,
            showResults = true,
            results = listOf(PhotoTile(1, PhotoImage.None)),
            hint = "Fotos com todas as pessoas selecionadas",
        ),
        previewLoader = null,
        actions = previewActions,
    )
}

// endregion
