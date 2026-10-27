package com.ipb.castelobranco.features.studies.domain.model

/** The structured body of a lesson (`backend/specs/sunday-school/spec.md`, "Content document"). */
data class LessonContent(
    val sections: List<LessonSection>,
)

data class LessonSection(
    val title: String,
    /** The name as the server sent it; kept so a save sends back an icon this app does not know. */
    val iconName: String?,
    val blocks: List<LessonBlock>,
) {
    /** Null when there is no icon or this app version does not know it: nothing is drawn. */
    val icon: LessonIcon? get() = iconName?.let { LessonIcon.fromWire(it) }
}

/** The closed icon list; each maps to a Phosphor duotone drawing in the app. */
enum class LessonIcon(val wireName: String) {
    HEART("heart"),
    SCALES("scales"),
    SHIELD("shield"),
    PLANT("plant"),
    HANDS_PRAYING("hands_praying"),
    CROWN("crown"),
    CROSS("cross"),
    BOOK_OPEN("book_open"),
    SCROLL("scroll"),
    FLAME("flame"),
    BIRD("bird"),
    CHURCH("church"),
    HOURGLASS("hourglass"),
    SWORD("sword"),
    MOUNTAINS("mountains"),
    LIGHTNING("lightning");

    companion object {
        fun fromWire(value: String): LessonIcon? = entries.firstOrNull { it.wireName == value }
    }
}

enum class ListStyle(val wireName: String) {
    BULLET("bullet"),
    NUMBERED("numbered"),
    ROMAN("roman");

    companion object {
        fun fromWire(value: String): ListStyle = entries.firstOrNull { it.wireName == value } ?: BULLET
    }
}

/**
 * One block of a section. Every field of the known types is modelled, so the editor can send a block it does not
 * edit back unchanged; a type this app does not know is kept as [Unknown] and skipped by the reader.
 */
sealed interface LessonBlock {

    data class Paragraph(
        val text: String,
        val proofTexts: List<ProofText> = emptyList(),
    ) : LessonBlock

    /** Wire type `list`. */
    data class BulletList(
        val style: ListStyle,
        val items: List<ListItem>,
    ) : LessonBlock

    data class KeyVerse(
        val label: String?,
        val text: String,
        val proof: ProofText,
    ) : LessonBlock

    data class Cards(val items: List<Card>) : LessonBlock

    data class Comparison(val items: List<ComparisonItem>) : LessonBlock

    data class Steps(val items: List<Step>) : LessonBlock

    data class Table(
        val header: List<String>,
        val rows: List<List<String>>,
    ) : LessonBlock

    data class Stats(val items: List<Stat>) : LessonBlock

    data class Timeline(val events: List<TimelineEvent>) : LessonBlock

    data class BookFactsBlock(
        val book: String,
        val author: String?,
        val date: String?,
        val style: String?,
        val readingTime: String?,
    ) : LessonBlock

    /** A block this app version cannot read: [rawJson] is the block exactly as received. */
    data class Unknown(
        val type: String,
        val rawJson: String,
    ) : LessonBlock

    /** Types the panel editor creates and edits in this version; the others are only moved or deleted. */
    val isEditable: Boolean get() = this is Paragraph || this is BulletList || this is KeyVerse
}

data class ListItem(
    val text: String,
    val proofTexts: List<ProofText> = emptyList(),
)

data class Card(
    val iconName: String?,
    val label: String?,
    val title: String,
    val text: String?,
    val proofTexts: List<ProofText>,
) {
    val icon: LessonIcon? get() = iconName?.let { LessonIcon.fromWire(it) }
}

data class ComparisonItem(
    val label: String,
    val title: String,
    val text: String?,
    val highlight: Boolean,
)

data class Step(
    val label: String?,
    val title: String,
    val text: String?,
)

data class Stat(
    val value: String,
    val label: String,
)

data class TimelineEvent(
    val whenLabel: String,
    val text: String,
)
