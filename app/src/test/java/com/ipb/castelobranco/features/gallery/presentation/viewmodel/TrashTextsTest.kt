package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.domain.trash.RestoreResult
import com.ipb.castelobranco.features.gallery.domain.trash.TrashEntry
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TrashTextsTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")
    private val today = LocalDate.of(2026, 10, 1)

    private fun entry(
        kind: TrashKind,
        deletedBy: String? = "Maria Souza",
        uploadedBy: String? = null,
        subAlbums: Int = 0,
        photos: Int = 0,
    ) = TrashEntry(
        kind = kind,
        id = 7,
        name = "Retiro 2026",
        deletedAt = "2026-09-29T14:03:11Z",
        deletedBy = deletedBy,
        uploadedBy = uploadedBy,
        purgeOn = "2026-10-29",
        subAlbumCount = subAlbums,
        photoCount = photos,
        thumbnailUrl = null,
    )

    @Test
    fun `album row - counts, deleter's first name, local day and purge day`() {
        val row = TrashTexts.row(entry(TrashKind.ALBUM, subAlbums = 2, photos = 41), saoPaulo, today)

        assertEquals("Retiro 2026", row.title)
        assertEquals("Álbum · 2 subálbuns · 41 fotos", row.kindLine)
        assertEquals("Apagado por Maria em 29/09 · some em 29/10", row.detailLine)
    }

    @Test
    fun `photo row with an unknown deleter`() {
        val row = TrashTexts.row(entry(TrashKind.PHOTO, deletedBy = null, uploadedBy = "Ana"), ZoneId.of("UTC"), today)

        assertEquals("Foto", row.kindLine)
        assertEquals("Apagado por usuário desconhecido em 29/09 · some em 29/10", row.detailLine)
    }

    @Test
    fun `soon means the purge day at most a week away`() {
        val album = entry(TrashKind.ALBUM)

        assertTrue(TrashTexts.row(album, saoPaulo, LocalDate.of(2026, 10, 22)).isPurgeSoon)
        assertFalse(TrashTexts.row(album, saoPaulo, LocalDate.of(2026, 10, 21)).isPurgeSoon)
    }

    @Test
    fun `band texts`() {
        assertEquals("1 item", TrashTexts.itemCount(1))
        assertEquals("5 itens", TrashTexts.itemCount(5))
        assertEquals(TrashTexts.NOTHING_SOON, TrashTexts.soonLine(0))
        assertEquals("1 some nesta semana", TrashTexts.soonLine(1))
        assertEquals("2 somem nesta semana", TrashTexts.soonLine(2))
    }

    @Test
    fun `counts use singulars and leave out zeros`() {
        assertEquals("1 subálbum · 1 foto", TrashTexts.counts(1, 1))
        assertEquals("3 fotos", TrashTexts.counts(0, 3))
        assertEquals("2 subálbuns", TrashTexts.counts(2, 0))
        assertNull(TrashTexts.counts(0, 0))
    }

    @Test
    fun `fractional seconds and offsets are read, anything else shown as it came`() {
        assertEquals("29/09/2026 14:03", TrashTexts.dateTime("2026-09-29T14:03:11.123456+00:00", ZoneId.of("UTC")))
        assertEquals("ontem", TrashTexts.dateTime("ontem", ZoneId.of("UTC")))
    }

    @Test
    fun `read errors - offline says the trash needs internet`() {
        assertEquals(TrashTexts.OFFLINE, TrashTexts.readError(AppError.Network()))
        val forbidden = AppError.Auth(code = 403, userMessage = "Sem permissão.")
        assertEquals("Sem permissão.", TrashTexts.readError(forbidden))
    }

    @Test
    fun `restore results`() {
        assertEquals("Álbum restaurado", TrashTexts.result(RestoreResult.Restored, TrashKind.ALBUM))
        assertEquals("Foto restaurada", TrashTexts.result(RestoreResult.Restored, TrashKind.PHOTO))
        assertEquals(TrashTexts.NOT_IN_TRASH, TrashTexts.result(RestoreResult.NotInTrash, TrashKind.PHOTO))
        val detail = AppError.Server(code = 400, userMessage = "Restaure o álbum 7 primeiro.")
        assertEquals(detail.userMessage, TrashTexts.result(RestoreResult.TrashedParent(7, detail), TrashKind.ALBUM))
    }
}
