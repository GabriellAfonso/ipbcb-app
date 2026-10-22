package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.screens

import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockPaginatorTest {

    private fun block(title: String? = null) = ChordBlock(title = title, lines = emptyList())

    // region empty input

    @Test
    fun `empty block list returns empty list`() {
        val result = BlockPaginator.paginate(
            blocks = emptyList(),
            blockHeights = emptyList(),
            availableHeight = 1000,
            blockSpacingPx = 10,
        )

        assertTrue(result.isEmpty())
    }

    // endregion

    // region single page

    @Test
    fun `blocks that fit in one page return single page`() {
        val blocks = listOf(block("A"), block("B"), block("C"))
        val heights = listOf(100, 100, 100)

        val result = BlockPaginator.paginate(
            blocks = blocks,
            blockHeights = heights,
            availableHeight = 500,
            blockSpacingPx = 10,
        )

        assertEquals(1, result.size)
        assertEquals(3, result[0].size)
    }

    @Test
    fun `all blocks summed exactly equal to availableHeight fit in one page`() {
        // heights: 200 + 10 (spacing) + 200 + 10 (spacing) + 80 = 500
        val blocks = listOf(block("A"), block("B"), block("C"))
        val heights = listOf(200, 200, 80)

        val result = BlockPaginator.paginate(
            blocks = blocks,
            blockHeights = heights,
            availableHeight = 500,
            blockSpacingPx = 10,
        )

        assertEquals(1, result.size)
    }

    @Test
    fun `zero-height blocks do not cause extra pagination`() {
        val blocks = listOf(block("A"), block("B"), block("C"))
        val heights = listOf(0, 0, 100)

        // spacing=0 so zero-height blocks add nothing to used height — C (100) still fits
        val result = BlockPaginator.paginate(
            blocks = blocks,
            blockHeights = heights,
            availableHeight = 100,
            blockSpacingPx = 0,
        )

        assertEquals(1, result.size)
        assertEquals(3, result[0].size)
    }

    @Test
    fun `blockSpacingPx zero does not add spacing between blocks`() {
        val blocks = listOf(block("A"), block("B"))
        val heights = listOf(300, 300)

        val result = BlockPaginator.paginate(
            blocks = blocks,
            blockHeights = heights,
            availableHeight = 600,
            blockSpacingPx = 0,
        )

        assertEquals(1, result.size)
    }

    // endregion

    // region multiple pages

    @Test
    fun `block exceeding available height goes to next page`() {
        val blocks = listOf(block("A"), block("B"))
        val heights = listOf(600, 600)

        val result = BlockPaginator.paginate(
            blocks = blocks,
            blockHeights = heights,
            availableHeight = 700,
            blockSpacingPx = 10,
        )

        assertEquals(2, result.size)
        assertEquals(1, result[0].size)
        assertEquals(1, result[1].size)
    }

    @Test
    fun `spacing is only counted between blocks on same page not on first block`() {
        // page 1: A(400) + spacing(10) + B(90) = 500 — exactly fills, no overflow
        // page 2: C(300) — starts without spacing (first block on new page)
        val blocks = listOf(block("A"), block("B"), block("C"))
        val heights = listOf(400, 90, 300)

        val result = BlockPaginator.paginate(
            blocks = blocks,
            blockHeights = heights,
            availableHeight = 500,
            blockSpacingPx = 10,
        )

        assertEquals(2, result.size)
        assertEquals(2, result[0].size) // A + B
        assertEquals(1, result[1].size) // C
    }

    @Test
    fun `single block larger than page goes in its own page without smart breaking`() {
        val blocks = listOf(block("A"))
        val heights = listOf(9999)

        val result = BlockPaginator.paginate(
            blocks = blocks,
            blockHeights = heights,
            availableHeight = 100,
            blockSpacingPx = 10,
        )

        assertEquals(1, result.size)
        assertEquals(1, result[0].size)
    }

    @Test
    fun `availableHeight zero puts each block in its own page`() {
        val blocks = listOf(block("A"), block("B"), block("C"))
        val heights = listOf(10, 20, 30)

        val result = BlockPaginator.paginate(
            blocks = blocks,
            blockHeights = heights,
            availableHeight = 0,
            blockSpacingPx = 0,
        )

        assertEquals(3, result.size)
        result.forEach { page -> assertEquals(1, page.size) }
    }

    // endregion
}
