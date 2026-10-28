package com.ipb.castelobranco.features.worshiphub.hub.presentation.state

import com.ipb.castelobranco.core.domain.setlist.SundaySetlist
import com.ipb.castelobranco.core.domain.setlist.setlistDateFor
import com.ipb.castelobranco.core.domain.worship.WorshipAccess
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
private val SAVED_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm")
private const val SUNDAY_PREFIX = "Domingo, "

/** One song of the band's setlist; [chordChartId] is null when the song has no chord chart. */
data class HubSongUi(
    val position: Int,
    val songId: Int,
    val title: String,
    val artist: String,
    val tone: String,
    val chordChartId: Int?,
)

/**
 * The hub's green band (`specs/worshiphub/spec.md` §1). Empty [songs] means no setlist saved yet.
 *
 * @param dateLabel "Domingo, 01/11". @param savedLabel "Salvo por Marcos · 27/10 às 21:40"; null when unreadable.
 * @param canDelete a setlist is shown and the user may save one: the same people may remove it.
 */
data class HubBandUi(
    val date: LocalDate,
    val dateLabel: String,
    val songs: List<HubSongUi>,
    val savedLabel: String?,
    val canSave: Boolean,
    val canDelete: Boolean = false,
)

/**
 * [band] is null for whoever is not a worship member: the Sunday setlist is only theirs. [isDeleting] while the
 * removal is on its way.
 */
data class WorshipHubUiState(val band: HubBandUi? = null, val isDeleting: Boolean = false)

fun setlistRemovedMessage(date: LocalDate) = "Repertório de domingo ${date.format(DAY_MONTH)} removido."

fun setlistRemoveFailedMessage(date: LocalDate) = "Não foi possível remover o repertório de ${date.format(DAY_MONTH)}."

fun removeSetlistTitle(date: LocalDate) = "Remover repertório de ${date.format(DAY_MONTH)}?"

/** @param chartIdBySongId the chord chart each song opens, as the chord charts' Sunday section picks it. */
fun buildHubBand(
    setlist: SundaySetlist?,
    access: WorshipAccess,
    chartIdBySongId: Map<Int, Int>,
    today: LocalDate,
    zone: ZoneId,
): HubBandUi? {
    if (!access.isWorshipMember) return null
    val date = setlist?.date ?: setlistDateFor(today)
    return HubBandUi(
        date = date,
        dateLabel = SUNDAY_PREFIX + date.format(DAY_MONTH),
        songs = setlist?.items.orEmpty().sortedBy { it.position }.map { item ->
            HubSongUi(
                position = item.position,
                songId = item.songId,
                title = item.title,
                artist = item.artist,
                tone = item.tone,
                chordChartId = chartIdBySongId[item.songId],
            )
        },
        savedLabel = setlist?.let { savedLabel(it.savedByName, it.savedAt, zone) },
        canSave = access.canSaveSetlist,
        canDelete = access.canSaveSetlist && setlist != null,
    )
}

private fun savedLabel(savedByName: String?, savedAt: String, zone: ZoneId): String? {
    val instant = runCatching { Instant.parse(savedAt) }
        .recoverCatching { OffsetDateTime.parse(savedAt).toInstant() }
        .getOrNull() ?: return null
    val whenSaved = instant.atZone(zone).format(SAVED_AT)
    return if (savedByName.isNullOrBlank()) "Salvo em $whenSaved" else "Salvo por $savedByName · $whenSaved"
}
