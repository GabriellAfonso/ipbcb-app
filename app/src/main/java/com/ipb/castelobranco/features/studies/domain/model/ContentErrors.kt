package com.ipb.castelobranco.features.studies.domain.model

import com.ipb.castelobranco.core.domain.error.AppError

/** One problem the server found in a lesson, by dotted path (`sections.2.blocks.1.paragraph.text`). */
data class ContentError(
    val path: String,
    val message: String,
)

/** Where in the form a [ContentError] is shown. */
sealed interface EditorLocation {
    data class Section(val section: Int) : EditorLocation
    data class Block(val section: Int, val block: Int) : EditorLocation

    /** Anything that cannot be pinned: shown at the top of the form. */
    data object Form : EditorLocation

    companion object {
        private const val SECTIONS = "sections"
        private const val BLOCKS = "blocks"

        /** `sections.<s>…` → section, `sections.<s>.blocks.<b>…` → block, anything else → [Form]. */
        fun of(path: String): EditorLocation {
            val parts = path.split('.')
            val section = parts.indexAfter(SECTIONS, 0) ?: return Form
            val block = if (parts.getOrNull(2) == BLOCKS) parts.indexAfter(BLOCKS, 2) else null
            return if (block != null) Block(section, block) else Section(section)
        }

        private fun List<String>.indexAfter(key: String, at: Int): Int? =
            if (getOrNull(at) == key) getOrNull(at + 1)?.toIntOrNull() else null
    }
}

/** The outcome of saving a lesson from the panel. */
sealed interface LessonSaveResult {
    data class Saved(val lesson: Lesson) : LessonSaveResult

    /** The server rejected the form: content problems by path and field problems by field name. */
    data class Invalid(
        val contentErrors: List<ContentError>,
        val fieldErrors: Map<String, List<String>>,
        val message: String?,
    ) : LessonSaveResult

    data class Failed(val error: AppError) : LessonSaveResult
}

/** The outcome of saving a year's lesson order. */
sealed interface ReorderResult {
    data object Saved : ReorderResult

    /** Someone else changed the year's lessons meanwhile. */
    data object Stale : ReorderResult

    data class Failed(val error: AppError) : ReorderResult
}
