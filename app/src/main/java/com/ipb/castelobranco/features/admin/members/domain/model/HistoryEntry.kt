package com.ipb.castelobranco.features.admin.members.domain.model

import java.time.Instant

/** One change to one field, as the server recorded it. [editor] null = account deleted since. */
data class HistoryEntry(
    val id: Int,
    val editor: HistoryEditor?,
    val field: String,
    val oldValue: String?,
    val newValue: String?,
    val changedAt: Instant,
)

data class HistoryEditor(val id: String, val name: String)

/** A history entry already turned into a Portuguese sentence. */
data class HistoryLine(
    val editorName: String,
    val text: String,
    val changedAt: Instant,
    /** The sentence without the values: "alterou Situação", "trocou a foto". */
    val action: String = text,
    /** A field change's old and new value, already in Portuguese; both null for the other entries. */
    val oldValue: String? = null,
    val newValue: String? = null,
    /** The editor's account was deleted. */
    val isEditorUnknown: Boolean = false,
)
