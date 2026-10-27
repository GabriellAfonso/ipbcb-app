package com.ipb.castelobranco.core.data.push

import com.ipb.castelobranco.core.domain.push.RecipientGender
import com.ipb.castelobranco.core.domain.push.ScheduleSlot
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Texts of the rota reminders (`specs/018-schedule-reminders/contracts/ui.md`). Formats follow the Escala screen
 * ("19h30", "quinta 29/10"), built here because `core` cannot use the schedule feature's helpers.
 */
object ScheduleNotificationText {

    const val WEEK_TITLE = "Você está na escala desta semana"

    fun weekText(slots: List<ScheduleSlot>): String =
        slots.singleOrNull()?.let(::line) ?: "${slots.size} cultos nesta semana"

    fun eveTitle(slot: ScheduleSlot): String = "Amanhã: ${slot.service}"

    fun eveText(slot: ScheduleSlot, gender: RecipientGender): String {
        val responsible = if (gender == RecipientGender.FEMALE) "a responsável" else "o responsável"
        return "Você é $responsible · ${slot.date.format(DAY_MONTH)} às ${time(slot.time)}"
    }

    fun publishedTitle(month: YearMonth): String = "Escala de ${monthName(month)} publicada"

    fun publishedText(count: Int, gender: RecipientGender): String {
        val scheduled = if (gender == RecipientGender.FEMALE) "escalada" else "escalado"
        return "Você está $scheduled em $count ${if (count == 1) "data" else "datas"}"
    }

    /** "Quinta de Oração · quinta 29/10 · 19h30". */
    fun line(slot: ScheduleSlot): String =
        "${slot.service} · ${weekday(slot.date)} ${slot.date.format(DAY_MONTH)} · ${time(slot.time)}"

    fun time(time: LocalTime): String = time.format(HOUR_MINUTE)

    /** "terça", "sábado", "domingo". */
    fun weekday(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.FULL, PT_BR).lowercase(PT_BR).removeSuffix(WEEKDAY_SUFFIX)

    fun monthName(month: YearMonth): String = month.month.getDisplayName(TextStyle.FULL, PT_BR).lowercase(PT_BR)

    private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
    private const val WEEKDAY_SUFFIX = "-feira"
    private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
    private val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH'h'mm")
}
