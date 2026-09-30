package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.domain.manage.BatchResult
import com.ipb.castelobranco.features.gallery.domain.model.SubtreeCounts
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryManageTextsTest {

    @Test
    fun `delete album leaves out what is empty and uses singulars`() {
        assertEquals(
            "Apagar 'Retiro' com 2 subálbuns e 41 fotos? Fica 30 dias na lixeira.",
            GalleryManageTexts.deleteAlbum("Retiro", SubtreeCounts(2, 41)),
        )
        assertEquals(
            "Apagar 'Culto' com 12 fotos? Fica 30 dias na lixeira.",
            GalleryManageTexts.deleteAlbum("Culto", SubtreeCounts(0, 12)),
        )
        assertEquals(
            "Apagar 'A' com 1 subálbum e 1 foto? Fica 30 dias na lixeira.",
            GalleryManageTexts.deleteAlbum("A", SubtreeCounts(1, 1)),
        )
        assertEquals(
            "Apagar 'Vazio'? Fica 30 dias na lixeira.",
            GalleryManageTexts.deleteAlbum("Vazio", SubtreeCounts(0, 0)),
        )
    }

    @Test
    fun `delete photos`() {
        assertEquals("Apagar 1 foto? Fica 30 dias na lixeira.", GalleryManageTexts.deletePhotos(1))
        assertEquals("Apagar 3 fotos? Ficam 30 dias na lixeira.", GalleryManageTexts.deletePhotos(3))
    }

    @Test
    fun `batch results group failures by reason`() {
        assertEquals("5 fotos movidas para 'Sábado'", GalleryManageTexts.moved(BatchResult(5, emptyList()), "Sábado"))
        assertEquals("1 foto apagada", GalleryManageTexts.deleted(BatchResult(1, emptyList())))

        val gone = AppError.Server(code = 404)
        val boom = AppError.Server(code = 500, userMessage = "Erro no servidor.")
        assertEquals(
            "1 de 4 fotos apagadas. 2: Este item não existe mais. 1: Erro no servidor.",
            GalleryManageTexts.deleted(BatchResult(1, listOf(gone, boom, gone))),
        )
    }
}
