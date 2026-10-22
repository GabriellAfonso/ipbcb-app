package com.ipb.castelobranco.core.domain.snapshot

interface Logger {
    fun warn(tag: String, message: String, throwable: Throwable? = null)

    data object Noop : Logger {
        override fun warn(tag: String, message: String, throwable: Throwable?) = Unit
    }
}
