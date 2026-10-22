package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.parser

data class LyricsStanza(val lines: List<String>)

object LyricsParser {

    fun parse(content: String): List<LyricsStanza> {
        val stanzas = mutableListOf<LyricsStanza>()
        val currentLines = mutableListOf<String>()

        fun flushStanza() {
            if (currentLines.isNotEmpty()) {
                stanzas += LyricsStanza(currentLines.toList())
                currentLines.clear()
            }
        }

        for (rawLine in content.lines()) {
            if (rawLine.trim().isEmpty()) {
                flushStanza()
            } else {
                currentLines += rawLine.trim()
            }
        }

        flushStanza()
        return stanzas
    }
}
