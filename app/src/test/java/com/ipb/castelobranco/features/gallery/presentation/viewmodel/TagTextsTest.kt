package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.domain.tags.TagBatchResult
import org.junit.Assert.assertEquals
import org.junit.Test

class TagTextsTest {

    @Test
    fun `photo counts are singular for one`() {
        assertEquals("1 foto", TagTexts.photoCount(1))
        assertEquals("3 fotos", TagTexts.photoCount(3))
    }

    @Test
    fun `a full batch counts the photos`() {
        assertEquals("Marcações atualizadas em 1 foto", TagTexts.updated(TagBatchResult(1, 1, null)))
        assertEquals("Marcações atualizadas em 250 fotos", TagTexts.updated(TagBatchResult(250, 250, null)))
    }

    @Test
    fun `a partial batch gives the count and the reason`() {
        val result = TagBatchResult(200, 250, AppError.Server(code = 404))

        assertEquals(
            "Marcações atualizadas em 200 de 250 fotos: ${TagTexts.NOT_FOUND}",
            TagTexts.updated(result),
        )
    }

    @Test
    fun `a batch where nothing went through gives only the reason`() {
        val offline = AppError.Network(userMessage = "Sem conexão")

        assertEquals("Sem conexão", TagTexts.updated(TagBatchResult(0, 3, offline)))
    }

    @Test
    fun `not found is the gone-meanwhile text, other refusals the server's message`() {
        assertEquals(
            "Algumas fotos ou pessoas não existem mais. Confira e tente de novo.",
            TagTexts.failure(AppError.Server(code = 404)),
        )
        val invalid = AppError.Server(code = 400, errorCode = "VALIDATION_ERROR", userMessage = "Lista inválida.")
        assertEquals("Lista inválida.", TagTexts.failure(invalid))
    }
}
