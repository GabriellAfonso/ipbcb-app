package com.ipb.castelobranco.features.studies.domain.usecase

import com.ipb.castelobranco.features.studies.domain.model.EditorLocation
import com.ipb.castelobranco.features.studies.domain.model.LessonBlock
import com.ipb.castelobranco.features.studies.domain.model.LessonContent
import com.ipb.castelobranco.features.studies.domain.model.LessonDraft
import com.ipb.castelobranco.features.studies.domain.model.LessonSection
import com.ipb.castelobranco.features.studies.domain.model.ProofText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateLessonDraftUseCaseTest {

    private val validate = ValidateLessonDraftUseCase()

    private fun draft(vararg blocks: LessonBlock, title: String = "Daniel", year: Int = 2026) =
        LessonDraft.new(year).copy(
            title = title,
            content = LessonContent(listOf(LessonSection("Contexto", "hourglass", blocks.toList()))),
        )

    @Test
    fun `a complete draft has no problems`() {
        assertTrue(validate(draft(LessonBlock.Paragraph("Texto"))).isEmpty)
    }

    @Test
    fun `blank title and year out of range are field problems`() {
        val problems = validate(draft(LessonBlock.Paragraph("Texto"), title = " ", year = 1999))

        assertEquals(setOf("title", "year"), problems.fields.keys)
    }

    @Test
    fun `empty paragraph and key verse without reference are pinned to their blocks`() {
        val problems = validate(
            draft(LessonBlock.Paragraph(""), LessonBlock.KeyVerse(null, "Texto", ProofText("", null))),
        )

        assertEquals(setOf(EditorLocation.Block(0, 0), EditorLocation.Block(0, 1)), problems.locations.keys)
    }

    @Test
    fun `blocks the editor does not edit are not checked`() {
        assertTrue(validate(draft(LessonBlock.Unknown("video", "{}"))).isEmpty)
    }

    @Test
    fun `the new lesson template is incomplete until written`() {
        val problems = validate(LessonDraft.new(2026).copy(title = "Gênesis"))

        assertEquals(6, problems.locations.size)
    }
}
