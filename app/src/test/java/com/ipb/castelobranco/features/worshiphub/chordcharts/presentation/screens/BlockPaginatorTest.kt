package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.screens

import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordBlock
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordLine
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.LineToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockPaginatorTest {

    private fun block(title: String? = null, lines: Int = 0) = ChordBlock(
        title = title,
        lines = List(lines) { ChordLine(listOf(LineToken.Lyrics("line $it"))) },
    )

    private fun whole(vararg totals: Int) = totals.map { BlockHeights(total = it) }

    private fun paginate(blocks: List<ChordBlock>, heights: List<BlockHeights>, available: Int, spacing: Int) =
        BlockPaginator.paginate(blocks, heights, available, spacing)

    // region whole blocks

    @Test
    fun `empty block list returns empty list`() {
        assertTrue(paginate(emptyList(), emptyList(), 1000, 10).isEmpty())
    }

    @Test
    fun `blocks that fit in one page return single page`() {
        val result = paginate(listOf(block("A"), block("B"), block("C")), whole(100, 100, 100), 500, 10)

        assertEquals(1, result.size)
        assertEquals(3, result[0].size)
    }

    @Test
    fun `all blocks summed exactly equal to availableHeight fit in one page`() {
        // 200 + 10 + 200 + 10 + 80 = 500
        val result = paginate(listOf(block("A"), block("B"), block("C")), whole(200, 200, 80), 500, 10)

        assertEquals(1, result.size)
    }

    @Test
    fun `zero-height blocks do not cause extra pagination`() {
        val result = paginate(listOf(block("A"), block("B"), block("C")), whole(0, 0, 100), 100, 0)

        assertEquals(1, result.size)
        assertEquals(3, result[0].size)
    }

    @Test
    fun `block exceeding available height goes to next page`() {
        val result = paginate(listOf(block("A"), block("B")), whole(600, 600), 700, 10)

        assertEquals(listOf(1, 1), result.map { it.size })
    }

    @Test
    fun `spacing is only counted between blocks on same page not on first block`() {
        // page 1: A(400) + 10 + B(90) = 500; page 2: C(300)
        val result = paginate(listOf(block("A"), block("B"), block("C")), whole(400, 90, 300), 500, 10)

        assertEquals(listOf(2, 1), result.map { it.size })
    }

    @Test
    fun `a tall block without line heights stays whole on its own page`() {
        val result = paginate(listOf(block("A", lines = 3)), whole(9999), 100, 10)

        assertEquals(1, result.size)
        assertEquals(3, result[0].single().block.lines.size)
    }

    @Test
    fun `availableHeight zero puts each block in its own page`() {
        val result = paginate(listOf(block("A"), block("B"), block("C")), whole(10, 20, 30), 0, 0)

        assertEquals(listOf(1, 1, 1), result.map { it.size })
    }

    // endregion

    // region tall blocks

    private val tall = BlockHeights(total = 520, head = 20, continuationHead = 8, lines = List(5) { 100 })

    @Test
    fun `a block taller than a page goes on in the next pages, line by line`() {
        // page 1: head 20 + 2 lines = 220; page 2: 8 + 2 lines = 208; page 3: 8 + 1 line
        val result = paginate(listOf(block("Coro", lines = 5)), listOf(tall), 250, 10)

        assertEquals(listOf(2, 2, 1), result.map { page -> page.single().block.lines.size })
        assertEquals("Coro", result[0].single().block.title)
        assertNull(result[1].single().block.title)
        assertEquals(listOf(0, 2, 4), result.map { it.single().position.line })
    }

    @Test
    fun `a tall block starts its own page and the next block can share its last one`() {
        val blocks = listOf(block("A"), block("Coro", lines = 5), block("B"))
        val heights = listOf(BlockHeights(total = 50), tall, BlockHeights(total = 30))

        val result = paginate(blocks, heights, 250, 10)

        assertEquals(listOf(0), result[0].map { it.position.block })
        assertEquals(listOf(1, 2), result.last().map { it.position.block })
    }

    @Test
    fun `a line taller than the page still gets a page`() {
        val heights = listOf(BlockHeights(total = 900, head = 20, continuationHead = 8, lines = listOf(400, 400)))

        val result = paginate(listOf(block("A", lines = 2)), heights, 100, 0)

        assertEquals(listOf(1, 1), result.map { it.single().block.lines.size })
    }

    @Test
    fun `pageOf finds the page that shows a position`() {
        val result = paginate(listOf(block("A"), block("Coro", lines = 5)), listOf(BlockHeights(50), tall), 250, 10)

        assertEquals(0, BlockPaginator.pageOf(result, PiecePosition(0, 0)))
        assertEquals(2, BlockPaginator.pageOf(result, PiecePosition(1, 2)))
        assertEquals(2, BlockPaginator.pageOf(result, PiecePosition(1, 3)))
        assertEquals(3, BlockPaginator.pageOf(result, PiecePosition(1, 4)))
    }

    // endregion
}
