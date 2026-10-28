package com.ipb.castelobranco.features.gallery.presentation.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GalleryCountLabelsTest {

    @Test
    fun `album band counts photos and videos`() {
        assertEquals("86 fotos · 4 vídeos", albumCountLabel(86, 4))
        assertEquals("1 foto · 1 vídeo", albumCountLabel(1, 1))
        assertEquals("23 fotos", albumCountLabel(23, 0))
        assertEquals("2 vídeos", albumCountLabel(0, 2))
        assertNull(albumCountLabel(0, 0))
    }

    @Test
    fun `root band joins the totals with e`() {
        assertEquals("549 fotos e 22 vídeos", galleryCountLabel(549, 22))
        assertEquals("1 foto", galleryCountLabel(1, 0))
        assertNull(galleryCountLabel(0, 0))
    }

    @Test
    fun `album count`() {
        assertEquals("1 álbum", albumsLabel(1))
        assertEquals("7 álbuns", albumsLabel(7))
    }

    @Test
    fun `album state counts only its own items`() {
        val state = AlbumUiState(
            isLoading = false,
            photos = listOf(
                PhotoTile(1, PhotoImage.None),
                PhotoTile(2, PhotoImage.None, isVideo = true),
                PhotoTile(3, PhotoImage.None),
            ),
        )

        assertEquals("2 fotos · 1 vídeo", state.countLabel)
    }
}
