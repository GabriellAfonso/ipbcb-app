package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.screens

import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordBlock

object BlockPaginator {

    /**
     * Splits [blocks] into pages that fit within [availableHeight] px,
     * with [blockSpacingPx] between consecutive blocks on the same page.
     */
    fun paginate(
        blocks: List<ChordBlock>,
        blockHeights: List<Int>,
        availableHeight: Int,
        blockSpacingPx: Int,
    ): List<List<ChordBlock>> {
        if (blocks.isEmpty()) return emptyList()

        val pages = mutableListOf<MutableList<ChordBlock>>()
        var currentPage = mutableListOf<ChordBlock>()
        var usedHeight = 0

        blocks.forEachIndexed { i, block ->
            val blockH = blockHeights[i]
            val spacing = if (currentPage.isEmpty()) 0 else blockSpacingPx
            if (currentPage.isNotEmpty() && usedHeight + spacing + blockH > availableHeight) {
                pages += currentPage
                currentPage = mutableListOf()
                usedHeight = 0
            }
            currentPage += block
            usedHeight += (if (usedHeight == 0) 0 else blockSpacingPx) + blockH
        }

        if (currentPage.isNotEmpty()) pages += currentPage
        return pages
    }
}
