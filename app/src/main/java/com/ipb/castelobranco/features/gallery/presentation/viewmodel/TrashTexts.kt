package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.gallery.domain.trash.RestoreResult
import com.ipb.castelobranco.features.gallery.domain.trash.TrashEntry
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKind
import com.ipb.castelobranco.features.gallery.presentation.state.TrashRow
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Every text of the trash and of a restore, shared by the trash screen and "Desfazer". */
internal object TrashTexts {

    const val TITLE = "Lixeira"
    const val EXPLANATION = "Os itens ficam 30 dias na lixeira e depois são apagados para sempre. " +
        "Restaurar um álbum traz de volta tudo o que foi apagado com ele."
    const val EMPTY = "A lixeira está vazia."
    const val OFFLINE = "Sem conexão. A lixeira precisa de internet."
    const val RETRY_LABEL = "Tentar novamente"
    const val RESTORE_LABEL = "Restaurar"
    const val OPEN_ALBUM_LABEL = "Abrir álbum"
    const val UNDO_LABEL = "Desfazer"
    const val NOT_IN_TRASH = "Este item não está mais na lixeira."
    const val ACCESS_LOST = "Você não tem mais acesso à lixeira."
    const val ALBUM_RESTORED = "Álbum restaurado"
    const val PHOTO_RESTORED = "Foto restaurada"
    const val SOON_GROUP = "Somem nesta semana"
    const val LATER_GROUP = "Mais tarde"
    const val NOTHING_SOON = "Nada some nesta semana"
    private const val KIND_ALBUM = "Álbum"
    private const val KIND_PHOTO = "Foto"
    private const val UNKNOWN_USER = "usuário desconhecido"

    /** "Nesta semana": the purge day at most this many days after today. */
    private const val SOON_DAYS = 7L

    private val DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM")

    /** [today] is the device's local date, which decides what goes "nesta semana". */
    fun row(entry: TrashEntry, zone: ZoneId, today: LocalDate): TrashRow {
        val purgeDay = date(entry.purgeOn)
        val kind = if (entry.kind == TrashKind.ALBUM) KIND_ALBUM else KIND_PHOTO
        val counts = if (entry.kind == TrashKind.ALBUM) counts(entry.subAlbumCount, entry.photoCount) else null
        val deleter = entry.deletedBy?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() } ?: UNKNOWN_USER
        val purge = purgeDay?.format(DAY_MONTH) ?: GalleryUiMapper.formatDate(entry.purgeOn)
        return TrashRow(
            key = entry.key,
            title = entry.name,
            kindLine = listOfNotNull(kind, counts).joinToString(" · "),
            detailLine = "Apagado por $deleter em ${dateTime(entry.deletedAt, zone, DAY_MONTH)} · some em $purge",
            isPurgeSoon = purgeDay != null && !purgeDay.isAfter(today.plusDays(SOON_DAYS)),
            thumbnailUrl = entry.thumbnailUrl,
        )
    }

    /** "1 item", "5 itens". */
    fun itemCount(count: Int): String = if (count == 1) "1 item" else "$count itens"

    /** The band's line: how many go within a week. */
    fun soonLine(count: Int): String = when (count) {
        0 -> NOTHING_SOON
        1 -> "1 some nesta semana"
        else -> "$count somem nesta semana"
    }

    /** An ISO instant in [zone] as `dd/MM/yyyy HH:mm` (or [format]); anything unreadable is shown as it came. */
    fun dateTime(raw: String, zone: ZoneId, format: DateTimeFormatter = DATE_TIME): String = try {
        OffsetDateTime.parse(raw).atZoneSameInstant(zone).format(format)
    } catch (_: DateTimeParseException) {
        raw
    }

    private fun date(raw: String): LocalDate? = try {
        LocalDate.parse(raw.take(ISO_DATE_LENGTH))
    } catch (_: DateTimeParseException) {
        null
    }

    private const val ISO_DATE_LENGTH = 10

    /** "{n} subálbuns · {m} fotos", singular forms, zero parts left out; `null` when both are zero. */
    fun counts(subAlbums: Int, photos: Int): String? = listOfNotNull(
        subAlbums.takeIf { it > 0 }?.let { if (it == 1) "1 subálbum" else "$it subálbuns" },
        photos.takeIf { it > 0 }?.let { if (it == 1) "1 foto" else "$it fotos" },
    ).takeIf { it.isNotEmpty() }?.joinToString(" · ")

    /** Why the trash could not be read. */
    fun readError(error: AppError): String = if (error is AppError.Network) OFFLINE else error.toUserMessage()

    /** The text of a restore's outcome. */
    fun result(result: RestoreResult, kind: TrashKind): String = when (result) {
        RestoreResult.Restored -> if (kind == TrashKind.ALBUM) ALBUM_RESTORED else PHOTO_RESTORED
        RestoreResult.NotInTrash -> NOT_IN_TRASH
        is RestoreResult.TrashedParent -> result.error.toUserMessage()
        is RestoreResult.NameConflict -> result.error.toUserMessage()
        is RestoreResult.Failed -> result.error.toUserMessage()
    }
}
