package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.screens

import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordBlock

/** Where a page piece starts: the block's index and the index of its first line. */
data class PiecePosition(val block: Int, val line: Int) : Comparable<PiecePosition> {
    override fun compareTo(other: PiecePosition): Int =
        compareValuesBy(this, other, PiecePosition::block, PiecePosition::line)
}

/** A whole block, or some of its lines when the block is taller than a page. */
data class PagePiece(val position: PiecePosition, val block: ChordBlock)

/**
 * A block's measured height. For a block taller than a page, also [head] (its tag), [continuationHead] (what a
 * piece without the tag adds) and each line's height in [lines]; `null` [lines] keeps the block whole.
 */
data class BlockHeights(
    val total: Int,
    val head: Int = 0,
    val continuationHead: Int = 0,
    val lines: List<Int>? = null,
)

object BlockPaginator {

    /**
     * Splits [blocks] into pages that fit within [availableHeight] px, with [blockSpacingPx] between consecutive
     * blocks on the same page. A block taller than a page starts a page and goes on in the next ones, line by line,
     * the tag only on its first piece.
     */
    fun paginate(
        blocks: List<ChordBlock>,
        heights: List<BlockHeights>,
        availableHeight: Int,
        blockSpacingPx: Int,
    ): List<List<PagePiece>> {
        if (blocks.isEmpty()) return emptyList()

        val pages = mutableListOf<List<PagePiece>>()
        var page = mutableListOf<PagePiece>()
        var usedHeight = 0

        fun newPage() {
            if (page.isNotEmpty()) pages += page
            page = mutableListOf()
            usedHeight = 0
        }

        blocks.forEachIndexed { index, block ->
            val height = heights[index]
            val lineHeights = height.lines
            val splits = lineHeights != null && height.total > availableHeight && block.lines.size > 1
            if (!splits) {
                val spacing = if (page.isEmpty()) 0 else blockSpacingPx
                if (page.isNotEmpty() && usedHeight + spacing + height.total > availableHeight) newPage()
                usedHeight += (if (page.isEmpty()) 0 else blockSpacingPx) + height.total
                page += PagePiece(PiecePosition(index, 0), block)
                return@forEachIndexed
            }

            newPage()
            var start = 0
            var head = height.head
            while (start < block.lines.size) {
                var end = start
                var used = head
                // At least one line per piece, even one taller than the page.
                while (end < block.lines.size && (end == start || used + lineHeights!![end] <= availableHeight)) {
                    used += lineHeights!![end]
                    end++
                }
                val lines = block.lines.subList(start, end)
                val piece = if (start == 0) block.copy(lines = lines) else block.copy(title = null, lines = lines)
                page += PagePiece(PiecePosition(index, start), piece)
                usedHeight = used
                start = end
                head = height.continuationHead
                if (start < block.lines.size) newPage()
            }
        }

        newPage()
        return pages
    }

    /** The page that shows [position]: where a reader was before the pages changed. */
    fun pageOf(pages: List<List<PagePiece>>, position: PiecePosition): Int =
        pages.indexOfLast { it.first().position <= position }.coerceAtLeast(0)
}
