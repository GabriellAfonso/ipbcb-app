package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.domain.trash.RestoreResult
import com.ipb.castelobranco.features.gallery.domain.trash.TrashEntry
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class TrashTextsTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")

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
    fun `album row - local time, counts and purge day`() {
        val row = TrashTexts.row(entry(TrashKind.ALBUM, subAlbums = 2, photos = 41), saoPaulo)

        assertEquals("Retiro 2026", row.title)
        assertEquals("Álbum", row.kindLabel)
        assertEquals("Apagado por Maria Souza em 29/09/2026 11:03", row.deletedLine)
        assertEquals("2 subálbuns · 41 fotos", row.countsLine)
        assertNull(row.uploadedLine)
        assertEquals("Some em 29/10/2026", row.purgeLine)
    }

    @Test
    fun `unknown deleter and uploader`() {
        val row = TrashTexts.row(entry(TrashKind.PHOTO, deletedBy = null, uploadedBy = null), ZoneId.of("UTC"))

        assertEquals("Foto", row.kindLabel)
        assertEquals("Apagado por usuário desconhecido em 29/09/2026 14:03", row.deletedLine)
        assertNull(row.uploadedLine)
        assertNull(row.countsLine)
    }

    @Test
    fun `photo row names its uploader`() {
        val row = TrashTexts.row(entry(TrashKind.PHOTO, uploadedBy = "Ana Paula"), saoPaulo)

        assertEquals("Enviada por Ana Paula", row.uploadedLine)
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
