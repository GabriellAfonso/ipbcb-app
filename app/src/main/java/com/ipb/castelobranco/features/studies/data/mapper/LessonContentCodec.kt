package com.ipb.castelobranco.features.studies.data.mapper

import com.ipb.castelobranco.features.studies.domain.model.BibleReference
import com.ipb.castelobranco.features.studies.domain.model.Card
import com.ipb.castelobranco.features.studies.domain.model.ComparisonItem
import com.ipb.castelobranco.features.studies.domain.model.LessonBlock
import com.ipb.castelobranco.features.studies.domain.model.LessonContent
import com.ipb.castelobranco.features.studies.domain.model.LessonSection
import com.ipb.castelobranco.features.studies.domain.model.ListItem
import com.ipb.castelobranco.features.studies.domain.model.ListStyle
import com.ipb.castelobranco.features.studies.domain.model.ProofText
import com.ipb.castelobranco.features.studies.domain.model.Stat
import com.ipb.castelobranco.features.studies.domain.model.Step
import com.ipb.castelobranco.features.studies.domain.model.TimelineEvent
import com.ipb.castelobranco.features.studies.domain.model.VerseSpan
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import timber.log.Timber

/**
 * Reads and writes the lesson content document (`specs/020-sunday-school/research.md` R-02). Each block is decoded
 * on its own: a type this app does not know, or a known type it cannot read, becomes [LessonBlock.Unknown] with the
 * block's JSON, which [encode] writes back as received.
 */
object LessonContentCodec {

    private const val SECTIONS = "sections"
    private const val TITLE = "title"
    private const val ICON = "icon"
    private const val BLOCKS = "blocks"
    private const val TYPE = "type"
    private const val TEXT = "text"
    private const val PROOF_TEXTS = "proof_texts"
    private const val PROOF = "proof"
    private const val PRINTED = "printed"
    private const val REFERENCE = "reference"
    private const val BOOK = "book"
    private const val CHAPTER = "chapter"
    private const val VERSES = "verses"
    private const val START = "start"
    private const val END = "end"
    private const val STYLE = "style"
    private const val ITEMS = "items"
    private const val LABEL = "label"
    private const val HIGHLIGHT = "highlight"
    private const val HEADER = "header"
    private const val ROWS = "rows"
    private const val VALUE = "value"
    private const val EVENTS = "events"
    private const val WHEN = "when"
    private const val AUTHOR = "author"
    private const val DATE = "date"
    private const val READING_TIME = "reading_time"

    const val TYPE_PARAGRAPH = "paragraph"
    const val TYPE_LIST = "list"
    const val TYPE_KEY_VERSE = "key_verse"
    const val TYPE_CARDS = "cards"
    const val TYPE_COMPARISON = "comparison"
    const val TYPE_STEPS = "steps"
    const val TYPE_TABLE = "table"
    const val TYPE_STATS = "stats"
    const val TYPE_TIMELINE = "timeline"
    const val TYPE_BOOK_FACTS = "book_facts"

    private val json = Json

    fun decode(content: JsonObject): LessonContent = LessonContent(
        sections = content.array(SECTIONS).mapNotNull { (it as? JsonObject)?.toSection() },
    )

    fun encode(content: LessonContent): JsonObject = buildJsonObject {
        put(SECTIONS, JsonArray(content.sections.map { it.toJson() }))
    }

    // ---- decode ----

    private fun JsonObject.toSection() = LessonSection(
        title = string(TITLE).orEmpty(),
        iconName = string(ICON),
        blocks = array(BLOCKS).mapNotNull { (it as? JsonObject)?.toBlock() },
    )

    private fun JsonObject.toBlock(): LessonBlock {
        val type = string(TYPE).orEmpty()
        return try {
            decodeKnown(type) ?: unknown(type)
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Unreadable lesson block of type %s", type)
            unknown(type)
        }
    }

    private fun JsonObject.unknown(type: String) = LessonBlock.Unknown(type = type, rawJson = toString())

    private fun JsonObject.decodeKnown(type: String): LessonBlock? = when (type) {
        TYPE_PARAGRAPH -> LessonBlock.Paragraph(text = required(TEXT), proofTexts = proofTexts())
        TYPE_LIST -> LessonBlock.BulletList(
            style = ListStyle.fromWire(string(STYLE).orEmpty()),
            items = objects(ITEMS).map { ListItem(text = it.required(TEXT), proofTexts = it.proofTexts()) },
        )
        TYPE_KEY_VERSE -> LessonBlock.KeyVerse(
            label = string(LABEL),
            text = required(TEXT),
            proof = (get(PROOF) as? JsonObject)?.toProofText()
                ?: throw IllegalArgumentException("key_verse without proof"),
        )
        TYPE_CARDS -> LessonBlock.Cards(
            items = objects(ITEMS).map {
                Card(
                    iconName = it.string(ICON),
                    label = it.string(LABEL),
                    title = it.required(TITLE),
                    text = it.string(TEXT),
                    proofTexts = it.proofTexts(),
                )
            },
        )
        TYPE_COMPARISON -> LessonBlock.Comparison(
            items = objects(ITEMS).map {
                ComparisonItem(
                    label = it.string(LABEL).orEmpty(),
                    title = it.required(TITLE),
                    text = it.string(TEXT),
                    highlight = (it[HIGHLIGHT] as? JsonPrimitive)?.booleanOrNull ?: false,
                )
            },
        )
        TYPE_STEPS -> LessonBlock.Steps(
            items = objects(ITEMS).map {
                Step(label = it.string(LABEL), title = it.required(TITLE), text = it.string(TEXT))
            },
        )
        TYPE_TABLE -> LessonBlock.Table(
            header = array(HEADER).map { it.stringValue() },
            rows = array(ROWS).map { row -> (row as? JsonArray).orEmpty().map { it.stringValue() } },
        )
        TYPE_STATS -> LessonBlock.Stats(
            items = objects(ITEMS).map { Stat(value = it.required(VALUE), label = it.required(LABEL)) },
        )
        TYPE_TIMELINE -> LessonBlock.Timeline(
            events = objects(EVENTS).map { TimelineEvent(whenLabel = it.required(WHEN), text = it.required(TEXT)) },
        )
        TYPE_BOOK_FACTS -> LessonBlock.BookFactsBlock(
            book = required(BOOK),
            author = string(AUTHOR),
            date = string(DATE),
            style = string(STYLE),
            readingTime = string(READING_TIME),
        )
        else -> null
    }

    private fun JsonObject.proofTexts(): List<ProofText> = objects(PROOF_TEXTS).map { it.toProofText() }

    private fun JsonObject.toProofText() = ProofText(
        printed = required(PRINTED),
        reference = (get(REFERENCE) as? JsonObject)?.let { reference ->
            BibleReference(
                book = reference.required(BOOK),
                chapter = (reference[CHAPTER] as? JsonPrimitive)?.intOrNull
                    ?: throw IllegalArgumentException("reference without chapter"),
                verses = reference.objects(VERSES).map {
                    VerseSpan(start = it.int(START), end = it.int(END))
                },
            )
        },
    )

    private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)
        ?.takeUnless { it is JsonNull }
        ?.contentOrNull

    private fun JsonObject.required(key: String): String =
        string(key) ?: throw IllegalArgumentException("missing '$key'")

    private fun JsonObject.int(key: String): Int =
        (get(key) as? JsonPrimitive)?.intOrNull ?: throw IllegalArgumentException("missing '$key'")

    private fun JsonObject.array(key: String): List<JsonElement> = (get(key) as? JsonArray).orEmpty()

    private fun JsonObject.objects(key: String): List<JsonObject> = array(key).filterIsInstance<JsonObject>()

    private fun JsonElement.stringValue(): String = (this as? JsonPrimitive)?.contentOrNull.orEmpty()

    // ---- encode ----

    private fun LessonSection.toJson() = buildJsonObject {
        put(TITLE, title)
        put(ICON, iconName)
        put(BLOCKS, JsonArray(blocks.map { it.toJson() }))
    }

    private fun LessonBlock.toJson(): JsonElement = when (this) {
        is LessonBlock.Paragraph -> buildJsonObject {
            put(TYPE, TYPE_PARAGRAPH)
            put(TEXT, text)
            put(PROOF_TEXTS, proofTexts.toJson())
        }
        is LessonBlock.BulletList -> buildJsonObject {
            put(TYPE, TYPE_LIST)
            put(STYLE, style.wireName)
            put(ITEMS, JsonArray(items.map { item ->
                buildJsonObject {
                    put(TEXT, item.text)
                    put(PROOF_TEXTS, item.proofTexts.toJson())
                }
            }))
        }
        is LessonBlock.KeyVerse -> buildJsonObject {
            put(TYPE, TYPE_KEY_VERSE)
            put(LABEL, label)
            put(TEXT, text)
            put(PROOF, proof.toJson())
        }
        is LessonBlock.Cards -> buildJsonObject {
            put(TYPE, TYPE_CARDS)
            put(ITEMS, JsonArray(items.map { card ->
                buildJsonObject {
                    put(ICON, card.iconName)
                    put(LABEL, card.label)
                    put(TITLE, card.title)
                    put(TEXT, card.text)
                    put(PROOF_TEXTS, card.proofTexts.toJson())
                }
            }))
        }
        is LessonBlock.Comparison -> buildJsonObject {
            put(TYPE, TYPE_COMPARISON)
            put(ITEMS, JsonArray(items.map { item ->
                buildJsonObject {
                    put(LABEL, item.label)
                    put(TITLE, item.title)
                    put(TEXT, item.text)
                    put(HIGHLIGHT, item.highlight)
                }
            }))
        }
        is LessonBlock.Steps -> buildJsonObject {
            put(TYPE, TYPE_STEPS)
            put(ITEMS, JsonArray(items.map { step ->
                buildJsonObject {
                    put(LABEL, step.label)
                    put(TITLE, step.title)
                    put(TEXT, step.text)
                }
            }))
        }
        is LessonBlock.Table -> buildJsonObject {
            put(TYPE, TYPE_TABLE)
            put(HEADER, JsonArray(header.map { JsonPrimitive(it) }))
            put(ROWS, JsonArray(rows.map { row -> JsonArray(row.map { JsonPrimitive(it) }) }))
        }
        is LessonBlock.Stats -> buildJsonObject {
            put(TYPE, TYPE_STATS)
            put(ITEMS, JsonArray(items.map { stat ->
                buildJsonObject {
                    put(VALUE, stat.value)
                    put(LABEL, stat.label)
                }
            }))
        }
        is LessonBlock.Timeline -> buildJsonObject {
            put(TYPE, TYPE_TIMELINE)
            put(EVENTS, JsonArray(events.map { event ->
                buildJsonObject {
                    put(WHEN, event.whenLabel)
                    put(TEXT, event.text)
                }
            }))
        }
        is LessonBlock.BookFactsBlock -> buildJsonObject {
            put(TYPE, TYPE_BOOK_FACTS)
            put(BOOK, book)
            put(AUTHOR, author)
            put(DATE, date)
            put(STYLE, style)
            put(READING_TIME, readingTime)
        }
        is LessonBlock.Unknown -> json.parseToJsonElement(rawJson)
    }

    private fun List<ProofText>.toJson() = JsonArray(map { it.toJson() })

    private fun ProofText.toJson() = buildJsonObject {
        put(PRINTED, printed)
        put(REFERENCE, reference?.let { reference ->
            buildJsonObject {
                put(BOOK, reference.book)
                put(CHAPTER, reference.chapter)
                put(VERSES, buildJsonArray {
                    reference.verses.forEach { span ->
                        add(buildJsonObject {
                            put(START, span.start)
                            put(END, span.end)
                        })
                    }
                })
            }
        } ?: JsonNull)
    }
}
