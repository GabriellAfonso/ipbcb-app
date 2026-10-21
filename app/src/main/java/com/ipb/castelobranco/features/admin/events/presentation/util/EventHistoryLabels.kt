package com.ipb.castelobranco.features.admin.events.presentation.util

import com.ipb.castelobranco.features.admin.events.domain.model.EventRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CHURCH_ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")
private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private const val SEPARATOR = " · "

/** "por Ana Paula" for the management list; null when the creator's name is unknown. */
fun EventRecord.authorLabel(): String? = createdByName?.let { "por $it" }

/**
 * "Criado por Ana Paula em 21/10/2026 · editado por Gabriel em 22/10/2026" (`specs/events/spec.md` §4.2). A part
 * without a name drops only the name; the edit part needs the server to have named an editor — it keeps only the
 * last one.
 */
fun EventRecord.historyLabel(): String? {
    val created = part("Criado", createdByName, createdAt)
    val edited = if (wasEdited) part("editado", updatedByName, updatedAt) else null
    return listOfNotNull(created, edited).joinToString(SEPARATOR).ifEmpty { null }?.replaceFirstChar { it.uppercase() }
}

private fun part(verb: String, name: String?, at: Instant?): String? {
    if (name == null && at == null) return null
    val by = name?.let { " por $it" }.orEmpty()
    val date = at?.let { " em ${DAY.format(it.atZone(CHURCH_ZONE))}" }.orEmpty()
    return verb + by + date
}
