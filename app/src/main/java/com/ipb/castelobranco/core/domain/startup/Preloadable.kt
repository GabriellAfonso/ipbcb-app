package com.ipb.castelobranco.core.domain.startup

fun interface Preloadable {
    suspend fun preload()
}
