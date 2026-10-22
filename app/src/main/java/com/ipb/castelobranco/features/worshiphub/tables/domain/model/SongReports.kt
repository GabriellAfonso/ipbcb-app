package com.ipb.castelobranco.features.worshiphub.tables.domain.model

data class SundaySet(
    val date: String, // manter "dd/MM/yyyy" por enquanto
    val songs: List<SundaySetItem>
)

data class SundaySetItem(
    val position: Int,
    val title: String,
    val artist: String,
    val tone: String,
    val songId: Int = 0,
)

data class TopSong(
    val title: String,
    val playCount: Int,
    val songId: Int = 0,
)

data class TopTone(
    val tone: String,
    val count: Int
)

data class SuggestedSong(
    val id: Int,
    val songId: Int,
    val title: String,
    val artist: String,
    val date: String, // "dd/MM/yyyy"
    val tone: String,
    val position: Int
)