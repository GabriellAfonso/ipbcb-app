// app/src/main/java/com/gabrielafonso/ipb/castelobranco/data/local/ProfilePhotoBus.kt
package com.ipb.castelobranco.features.profile.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ProfilePhotoBus {
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    fun bump() {
        _version.value = _version.value + 1
    }
}
