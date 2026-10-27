package com.ipb.castelobranco.features.studies.data.mapper

import com.ipb.castelobranco.features.studies.domain.model.LessonBlock
import com.ipb.castelobranco.features.studies.domain.model.LessonIcon
import com.ipb.castelobranco.features.studies.domain.model.ListStyle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonContentCodecTest {

    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test
    fun `decodes every known block type`() {
        val content = LessonContentCodec.decode(parse(ALL_TYPES))

        val section = content.sections.single()
        assertEquals("Contexto", section.title)
        assertEquals(LessonIcon.HOURGLASS, section.icon)
        val blocks = section.blocks
        assertEquals(10, blocks.size)
        val paragraph = blocks[0] as LessonBlock.Paragraph
        assertEquals("Mt", paragraph.proofTexts.single().reference?.book)
        assertEquals(ListStyle.ROMAN, (blocks[1] as LessonBlock.BulletList).style)
        assertNull((blocks[2] as LessonBlock.KeyVerse).proof.reference)
        assertEquals(LessonIcon.CROWN, (blocks[3] as LessonBlock.Cards).items.single().icon)
        assertTrue((blocks[4] as LessonBlock.Comparison).items[1].highlight)
        assertEquals("Exílio", (blocks[5] as LessonBlock.Steps).items.single().title)
        assertEquals(listOf("", "b"), (blocks[6] as LessonBlock.Table).rows.single())
        assertEquals("12", (blocks[7] as LessonBlock.Stats).items.single().value)
        assertEquals("605 a.C.", (blocks[8] as LessonBlock.Timeline).events.single().whenLabel)
        assertEquals("Dn", (blocks[9] as LessonBlock.BookFactsBlock).book)
    }

    @Test
    fun `an unknown type or an unreadable block becomes Unknown and is written back as received`() {
        val raw = """{"sections":[{"title":"S","icon":"dove","blocks":[
            {"type":"video","url":"x"},
            {"type":"paragraph"}
        ]}]}"""
        val content = LessonContentCodec.decode(parse(raw))

        val section = content.sections.single()
        assertEquals("video", (section.blocks[0] as LessonBlock.Unknown).type)
        assertEquals("paragraph", (section.blocks[1] as LessonBlock.Unknown).type)
        assertNull(section.icon)

        assertEquals(parse(raw), LessonContentCodec.encode(content))
    }

    @Test
    fun `encode then decode keeps every field`() {
        val content = LessonContentCodec.decode(parse(ALL_TYPES))

        assertEquals(content, LessonContentCodec.decode(LessonContentCodec.encode(content)))
    }

    @Test
    fun `a normalized document is encoded back unchanged`() {
        assertEquals(parse(ALL_TYPES), LessonContentCodec.encode(LessonContentCodec.decode(parse(ALL_TYPES))))
    }

    private companion object {
        val ALL_TYPES = """
            {"sections":[{"title":"Contexto","icon":"hourglass","blocks":[
              {"type":"paragraph","text":"O **Exílio**","proof_texts":[
                {"printed":"Mateus 24.15","reference":{"book":"Mt","chapter":24,"verses":[{"start":15,"end":15}]}}]},
              {"type":"list","style":"roman","items":[{"text":"um","proof_texts":[]}]},
              {"type":"key_verse","label":null,"text":"Daniel resolveu","proof":{"printed":"Dn 1.8","reference":null}},
              {"type":"cards","items":[{"icon":"crown","label":null,"title":"Reino","text":null,"proof_texts":[]}]},
              {"type":"comparison","items":[
                {"label":"A","title":"Babilônia","text":null,"highlight":false},
                {"label":"B","title":"Deus","text":"eterno","highlight":true}]},
              {"type":"steps","items":[{"label":null,"title":"Exílio","text":null}]},
              {"type":"table","header":["a","b"],"rows":[["","b"]]},
              {"type":"stats","items":[{"value":"12","label":"capítulos"}]},
              {"type":"timeline","events":[{"when":"605 a.C.","text":"Cativeiro"}]},
              {"type":"book_facts","book":"Dn","author":"Daniel","date":null,"style":null,"reading_time":"2 h"}
            ]}]}
        """.trimIndent()
    }
}
