package com.ipb.castelobranco.features.worshiphub.chordcharts.domain.model

data class ChordChart(
    val id: Int,
    val songId: Int,
    val content: String,
    val tone: String,
    val instrument: String,
)
