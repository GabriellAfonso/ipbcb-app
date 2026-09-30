package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.domain.manage.GalleryWriteError
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryWriteErrorsTest {

    @Test
    fun `network error is offline, with the text the repository set`() {
        val error = AppError.Network(userMessage = "Sem conexão").toGalleryWriteError()

        assertEquals(GalleryWriteError.Offline("Sem conexão"), error)
    }

    @Test
    fun `403 is forbidden with the server detail`() {
        val error = AppError.Auth(code = 403, userMessage = "Sem permissão na galeria.").toGalleryWriteError()

        assertEquals(GalleryWriteError.Forbidden("Sem permissão na galeria."), error)
    }

    @Test
    fun `404 is not found`() {
        assertEquals(GalleryWriteError.NotFound(NOT_FOUND_MESSAGE), AppError.Server(code = 404).toGalleryWriteError())
    }

    @Test
    fun `400 with chain is a cycle`() {
        val error = AppError.Server(
            code = 400,
            userMessage = "Não é possível mover.",
            extras = mapOf("chain" to "[9,3]"),
        )

        assertEquals(GalleryWriteError.Cycle("Não é possível mover."), error.toGalleryWriteError())
    }

    @Test
    fun `400 with missing, unexpected or repeated is an order mismatch with the Portuguese text`() {
        listOf("missing", "unexpected", "repeated").forEach { key ->
            val error = AppError.Server(code = 400, userMessage = "Order must list…", extras = mapOf(key to "[1]"))

            assertEquals(GalleryWriteError.OrderMismatch(ORDER_CHANGED_MESSAGE), error.toGalleryWriteError())
        }
    }

    @Test
    fun `another structured 400 is a duplicate name with the server detail`() {
        val error = AppError.Server(code = 400, errorCode = "VALIDATION_ERROR", userMessage = "Já existe um álbum.")

        assertEquals(GalleryWriteError.DuplicateName("Já existe um álbum."), error.toGalleryWriteError())
    }

    @Test
    fun `anything else is other`() {
        val error = AppError.Server(code = 500).toGalleryWriteError()

        assertEquals(true, error is GalleryWriteError.Other)
    }
}
