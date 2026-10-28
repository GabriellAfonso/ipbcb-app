package com.ipb.castelobranco.features.studies.presentation.components.sundayschool

import org.junit.Assert.assertEquals
import org.junit.Test

class LessonTableTest {

    @Test
    fun `columns are measured on the visible text`() {
        val columns = tableColumns(
            header = listOf("Seção", "Capítulos", "Foco"),
            rows = listOf(listOf("I. A Conquista", "1–12", "**Guerra Teocêntrica:** A entrada na terra")),
        )

        assertEquals(listOf(14f, 9f, 38f), columns.map { it.share })
        assertEquals(listOf(9, 9, 12), columns.map { it.longestWord })
    }

    @Test
    fun `a share is clamped so one huge cell does not squeeze the rest`() {
        val columns = tableColumns(header = listOf("A", "B"), rows = listOf(listOf("x", "y".repeat(200))))

        assertEquals(listOf(6f, 60f), columns.map { it.share })
    }

    @Test
    fun `a short column keeps its minimum and the rest goes to the long one`() {
        val widths = fitColumnWidths(shares = listOf(18f, 9f, 60f), mins = listOf(110f, 85f, 110f), available = 350f)

        assertEquals(listOf(110f, 85f, 155f), widths)
    }

    @Test
    fun `columns that all fit split the width by share`() {
        val widths = fitColumnWidths(shares = listOf(10f, 30f), mins = listOf(20f, 20f), available = 400f)

        assertEquals(listOf(100f, 300f), widths)
    }

    @Test
    fun `minimums wider than the screen are kept so the table scrolls`() {
        val widths = fitColumnWidths(shares = listOf(1f, 1f), mins = listOf(300f, 300f), available = 400f)

        assertEquals(listOf(300f, 300f), widths)
    }
}
