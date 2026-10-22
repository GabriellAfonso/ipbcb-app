package com.ipb.castelobranco.features.schedule.domain.formatter

import com.ipb.castelobranco.features.schedule.domain.model.MonthSchedule
import com.ipb.castelobranco.features.schedule.domain.model.ScheduleEntry
import com.ipb.castelobranco.features.schedule.domain.model.ScheduleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthScheduleWhatsappFormatterTest {

    // region monthPtBr

    @Test
    fun `monthPtBr returns correct name for all 12 months`() {
        val expected = mapOf(
            1 to "Janeiro", 2 to "Fevereiro", 3 to "Março",
            4 to "Abril", 5 to "Maio", 6 to "Junho",
            7 to "Julho", 8 to "Agosto", 9 to "Setembro",
            10 to "Outubro", 11 to "Novembro", 12 to "Dezembro"
        )
        expected.forEach { (month, name) ->
            assertEquals("Month $month", name, MonthScheduleWhatsappFormatter.monthPtBr(month))
        }
    }

    @Test
    fun `monthPtBr returns Mes for value 0`() {
        assertEquals("Mês", MonthScheduleWhatsappFormatter.monthPtBr(0))
    }

    @Test
    fun `monthPtBr returns Mes for value 13`() {
        assertEquals("Mês", MonthScheduleWhatsappFormatter.monthPtBr(13))
    }

    // endregion

    // region header

    @Test
    fun `format produces correct header`() {
        val schedule = MonthSchedule(year = 2025, month = 1, schedule = emptyMap())

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        assertTrue(result.startsWith("ESCALA DE JANEIRO 2025 - DIRIGENTES E RESPONSÁVEIS"))
    }

    @Test
    fun `format header uses uppercase locale pt-BR for accented month`() {
        val schedule = MonthSchedule(year = 2025, month = 3, schedule = emptyMap())

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        assertTrue(result.startsWith("ESCALA DE MARÇO 2025 - DIRIGENTES E RESPONSÁVEIS"))
    }

    // endregion

    // region footer

    @Test
    fun `format ends with DEUS ABENCOE`() {
        val schedule = MonthSchedule(year = 2025, month = 1, schedule = emptyMap())

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        assertTrue(result.endsWith("DEUS ABENÇOE"))
    }

    @Test
    fun `format contains cafezinho footer line`() {
        val schedule = MonthSchedule(year = 2025, month = 1, schedule = emptyMap())

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        assertTrue(result.contains("\\*Cafezinho pós culto de Adoração (Ceia) todo 4° Domingo"))
    }

    @Test
    fun `format contains aberto footer line`() {
        val schedule = MonthSchedule(year = 2025, month = 1, schedule = emptyMap())

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        assertTrue(result.contains("\\* Aberto a participação de qualquer irmão."))
    }

    // endregion

    // region section ordering

    @Test
    fun `sections are ordered terca before quinta before domingo before others`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Domingo" to ScheduleEntry(time = "", items = listOf(ScheduleItem(1, "A"))),
                "Louvor" to ScheduleEntry(time = "", items = listOf(ScheduleItem(2, "B"))),
                "Quinta-feira" to ScheduleEntry(time = "", items = listOf(ScheduleItem(3, "C"))),
                "Terça-feira" to ScheduleEntry(time = "", items = listOf(ScheduleItem(4, "D"))),
            )
        )

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        val tercaIdx = result.indexOf("Terça-feira")
        val quintaIdx = result.indexOf("Quinta-feira")
        val domingoIdx = result.indexOf("Domingo")
        val louvorIdx = result.indexOf("Louvor")

        assertTrue(tercaIdx < quintaIdx)
        assertTrue(quintaIdx < domingoIdx)
        assertTrue(domingoIdx < louvorIdx)
    }

    @Test
    fun `sections with same weight are sorted alphabetically`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Zebrinha" to ScheduleEntry(time = "", items = listOf(ScheduleItem(1, "A"))),
                "Abertura" to ScheduleEntry(time = "", items = listOf(ScheduleItem(2, "B"))),
                "Louvor" to ScheduleEntry(time = "", items = listOf(ScheduleItem(3, "C"))),
            )
        )

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        val aberturaIdx = result.indexOf("Abertura")
        val louvorIdx = result.indexOf("Louvor")
        val zebrinhaIdx = result.indexOf("Zebrinha")

        assertTrue(aberturaIdx < louvorIdx)
        assertTrue(louvorIdx < zebrinhaIdx)
    }

    // endregion

    // region time display

    @Test
    fun `section with non-blank time shows time in parentheses`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Terça-feira" to ScheduleEntry(
                    time = "19h30",
                    items = listOf(ScheduleItem(1, "Ana"))
                )
            )
        )

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        assertTrue(result.contains("Terça-feira (19h30)"))
    }

    @Test
    fun `section with blank time omits parentheses`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Terça-feira" to ScheduleEntry(
                    time = "",
                    items = listOf(ScheduleItem(1, "Ana"))
                )
            )
        )

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        assertFalse(result.contains("Terça-feira ("))
        assertTrue(result.contains("Terça-feira\n"))
    }

    // endregion

    // region items ordering and formatting

    @Test
    fun `items within a section are ordered by day`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Domingo" to ScheduleEntry(
                    time = "",
                    items = listOf(
                        ScheduleItem(15, "Carlos"),
                        ScheduleItem(1, "Ana"),
                        ScheduleItem(8, "Bruno"),
                    )
                )
            )
        )

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        val anaIdx = result.indexOf("Ana")
        val brunoIdx = result.indexOf("Bruno")
        val carlosIdx = result.indexOf("Carlos")

        assertTrue(anaIdx < brunoIdx)
        assertTrue(brunoIdx < carlosIdx)
    }

    @Test
    fun `single-digit day is padded with leading zero`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Domingo" to ScheduleEntry(
                    time = "",
                    items = listOf(ScheduleItem(5, "Ana"))
                )
            )
        )

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        assertTrue(result.contains("05- Ana"))
    }

    @Test
    fun `two-digit day is not padded`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Domingo" to ScheduleEntry(
                    time = "",
                    items = listOf(ScheduleItem(15, "Ana"))
                )
            )
        )

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        assertTrue(result.contains("15- Ana"))
    }

    // endregion

    // region edge cases

    @Test
    fun `section with no items does not crash`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Domingo" to ScheduleEntry(time = "", items = emptyList())
            )
        )

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        assertTrue(result.contains("Domingo"))
    }

    @Test
    fun `schedule with single section formats correctly`() {
        val schedule = MonthSchedule(
            year = 2025, month = 6,
            schedule = mapOf(
                "Terça-feira" to ScheduleEntry(
                    time = "20h",
                    items = listOf(ScheduleItem(3, "João"), ScheduleItem(10, "Maria"))
                )
            )
        )

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        assertTrue(result.contains("ESCALA DE JUNHO 2025"))
        assertTrue(result.contains("Terça-feira (20h)"))
        assertTrue(result.contains("03- João"))
        assertTrue(result.contains("10- Maria"))
        assertTrue(result.endsWith("DEUS ABENÇOE"))
    }

    @Test
    fun `empty schedule still produces header and footer`() {
        val schedule = MonthSchedule(year = 2025, month = 1, schedule = emptyMap())

        val result = MonthScheduleWhatsappFormatter.format(schedule)

        assertTrue(result.startsWith("ESCALA DE JANEIRO 2025"))
        assertTrue(result.endsWith("DEUS ABENÇOE"))
    }

    // endregion
}
