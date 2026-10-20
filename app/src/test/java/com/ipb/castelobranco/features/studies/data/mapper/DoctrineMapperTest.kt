package com.ipb.castelobranco.features.studies.data.mapper

import com.ipb.castelobranco.features.studies.DoctrineTestFixtures
import com.ipb.castelobranco.features.studies.data.dto.DoctrineDocumentDto
import com.ipb.castelobranco.features.studies.data.dto.DoctrineListDto
import com.ipb.castelobranco.features.studies.domain.model.DoctrineDocument
import com.ipb.castelobranco.features.studies.domain.model.DoctrineKind
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoctrineMapperTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun `list keeps server order, drops unknown kinds and adds short titles and labels`() {
        val list = json.decodeFromString<DoctrineListDto>(DoctrineTestFixtures.listJson).toDomain()

        assertEquals(listOf(DoctrineTestFixtures.SHORTER, DoctrineTestFixtures.CONFESSION), list.map { it.slug })
        assertEquals("Breve Catecismo", list[0].shortTitle)
        assertEquals("107 perguntas e respostas", list[0].itemLabel)
        assertEquals(DoctrineKind.CONFESSION, list[1].kind)
        assertEquals("33 capítulos", list[1].itemLabel)
    }

    @Test
    fun `catechism is sorted by number and keeps answer line breaks and null references`() {
        val document = json.decodeFromString<DoctrineDocumentDto>(DoctrineTestFixtures.catechismJson).toDomain()

        assertTrue(document is DoctrineDocument.Catechism)
        val questions = (document as DoctrineDocument.Catechism).questions
        assertEquals(listOf(1, 2), questions.map { it.number })
        assertEquals("A Palavra de Deus.\n1ª. Primeira regra", questions[1].answer)
        assertNull(questions[1].proofTexts.single().reference)
        assertEquals(36, questions[0].proofTexts[0].reference!!.firstVerse)
        assertEquals(1, questions[0].proofTexts[1].reference!!.firstVerse)
    }

    @Test
    fun `confession titles become sentence case and missing lists become empty`() {
        val document = json.decodeFromString<DoctrineDocumentDto>(DoctrineTestFixtures.confessionJson).toDomain()

        val chapter = (document as DoctrineDocument.Confession).chapters.single()
        assertEquals("Da Sagradas Escrituras", chapter.title)
        assertEquals(emptyList<Any>(), chapter.paragraphs[0].lists)
        assertEquals("O Novo Testamento", chapter.paragraphs[1].lists[1].heading)
    }

    @Test
    fun `document of an unknown kind maps to null`() {
        val dto = DoctrineDocumentDto(slug = "x", title = "X", kind = "devotional")

        assertNull(dto.toDomain())
    }
}
