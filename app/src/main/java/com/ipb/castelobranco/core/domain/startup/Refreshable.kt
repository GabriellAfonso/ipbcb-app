package com.ipb.castelobranco.core.domain.startup

fun interface Refreshable {
    suspend fun refresh()
}
