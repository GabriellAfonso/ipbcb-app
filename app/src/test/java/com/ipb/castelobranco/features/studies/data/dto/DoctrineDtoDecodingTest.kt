package com.ipb.castelobranco.features.studies.data.dto

import com.ipb.castelobranco.features.studies.DoctrineTestFixtures
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DoctrineDtoDecodingTest {

    // Same settings as SerializationModule.provideJson().
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `catechism decodes questions, proof texts and null references`() {
        val dto = json.decodeFromString<DoctrineDocumentDto>(DoctrineTestFixtures.catechismJson)

        assertEquals("catechism", dto.kind)
        assertNull(dto.chapters)
        val q1 = dto.questions!!.first { it.number == 1 }
        assertEquals(2, q1.proofTexts.size)
        assertEquals(36, q1.proofTexts[0].reference!!.verses.single().start)
        assertEquals(emptyList<VerseSpanDto>(), q1.proofTexts[1].reference!!.verses)
        assertNull(dto.questions.first { it.number == 2 }.proofTexts.single().reference)
    }

    @Test
    fun `confession decodes lists and text after, absent fields stay null`() {
        val dto = json.decodeFromString<DoctrineDocumentDto>(DoctrineTestFixtures.confessionJson)

        val paragraphs = dto.chapters!!.single().paragraphs
        assertNull(paragraphs[0].lists)
        assertNull(paragraphs[0].textAfter)
        assertEquals(2, paragraphs[1].lists!!.size)
        assertEquals("Todos dados por inspiração de Deus.", paragraphs[1].textAfter)
    }

    @Test
    fun `list decodes item count and survives a cached copy round trip`() {
        val dto = json.decodeFromString<DoctrineListDto>(DoctrineTestFixtures.listJson)
        val again = json.decodeFromString<DoctrineListDto>(json.encodeToString(DoctrineListDto.serializer(), dto))

        assertEquals(107, dto.documents.first().itemCount)
        assertEquals(dto, again)
    }
}
