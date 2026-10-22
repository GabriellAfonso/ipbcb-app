package com.ipb.castelobranco.features.schedule.domain.formatter

import com.ipb.castelobranco.features.schedule.domain.model.MonthSchedule
import com.ipb.castelobranco.features.schedule.domain.model.ScheduleEntry
import java.util.Locale

object MonthScheduleWhatsappFormatter {

    fun format(schedule: MonthSchedule, locale: Locale = Locale.forLanguageTag("pt-BR")): String {
        val monthName = monthPtBr(schedule.month).uppercase(locale)

        val sb = StringBuilder()
        sb.append("ESCALA DE ")
            .append(monthName)
            .append(" ")
            .append(schedule.year)
            .append(" - DIRIGENTES E RESPONSÁVEIS\n\n")

        val order = listOf("terça", "terca", "quinta", "domingo")

        val entriesSorted = schedule.schedule.entries
            .sortedWith(
                compareBy<Map.Entry<String, ScheduleEntry>> { entry ->
                    val t = entry.key.trim().lowercase(locale)
                    order.indexOfFirst { t.startsWith(it) }
                        .let { if (it == -1) Int.MAX_VALUE else it }
                }.thenBy { it.key.lowercase(locale) }
            )

        entriesSorted.forEach { (title, entry) ->
            sb.append(title)
            entry.time.takeIf { it.isNotBlank() }?.let { sb.append(" (").append(it).append(")") }
            sb.append("\n\n")

            entry.items
                .sortedBy { it.day }
                .forEach { item ->
                    sb.append(String.format("%02d", item.day))
                        .append("- ")
                        .append(item.member)
                        .append("\n")
                }

            sb.append("\n")
        }

        sb.append("\\*Cafezinho pós culto de Adoração (Ceia) todo 4° Domingo\n\n")
        sb.append("\\* Aberto a participação de qualquer irmão.\n\n")
        sb.append("DEUS ABENÇOE")

        return sb.toString().trim()
    }

    // Agora é público para a UI conseguir usar no título
    fun monthPtBr(month: Int): String =
        when (month) {
            1 -> "Janeiro"
            2 -> "Fevereiro"
            3 -> "Março"
            4 -> "Abril"
            5 -> "Maio"
            6 -> "Junho"
            7 -> "Julho"
            8 -> "Agosto"
            9 -> "Setembro"
            10 -> "Outubro"
            11 -> "Novembro"
            12 -> "Dezembro"
            else -> "Mês"
        }
}