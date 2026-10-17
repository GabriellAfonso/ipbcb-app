package com.ipb.castelobranco.features.settings.presentation.components

import java.util.Locale

private const val BYTES_PER_UNIT = 1024.0
private const val ONE_DECIMAL_BELOW = 10.0
private const val EMPTY_LABEL = "nada no aparelho"
private const val SEPARATOR = " · "
private val units = listOf("B", "KB", "MB", "GB")
private val ptBr = Locale.forLanguageTag("pt-BR")

/** `8,4 MB`, `312 MB`, `nada no aparelho` for zero. */
fun formatDiskUsage(bytes: Long): String {
    if (bytes <= 0) return EMPTY_LABEL
    var value = bytes.toDouble()
    var unit = 0
    while (value >= BYTES_PER_UNIT && unit < units.lastIndex) {
        value /= BYTES_PER_UNIT
        unit++
    }
    val number = if (unit > 0 && value < ONE_DECIMAL_BELOW) {
        String.format(ptBr, "%.1f", value)
    } else {
        String.format(ptBr, "%.0f", value)
    }
    return "$number ${units[unit]}"
}

/** [description] followed by the size once it is known. */
fun withDiskUsage(description: String, bytes: Long?): String =
    if (bytes == null) description else description + SEPARATOR + formatDiskUsage(bytes)
