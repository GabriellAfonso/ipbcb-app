package com.ipb.castelobranco.features.gallery.presentation.state

import com.ipb.castelobranco.features.gallery.domain.model.VideoDuration
import java.math.BigDecimal
import java.math.RoundingMode

/** A time typed on the editor: `1:23`, `1:02:03`, `83` or `83,5` (seconds, comma or point). */
object TrimTime {
    private const val SEPARATOR = ':'
    private const val MAX_PARTS = 3
    private const val SECONDS_PER_UNIT = 60
    private const val MS_PER_SECOND = 1_000L
    private const val MS_PER_TENTH = 100L
    private const val DECIMAL_COMMA = ','
    private const val DECIMAL_POINT = '.'

    /** Milliseconds, or null when [text] is not a time. Below the first part, a part must stay under 60. */
    fun parse(text: String): Long? {
        val parts = text.trim().replace(DECIMAL_COMMA, DECIMAL_POINT).split(SEPARATOR)
        if (parts.size > MAX_PARTS || parts.any { it.isBlank() }) return null
        val seconds = parts.last().toBigDecimalOrNull()?.takeIf { it.signum() >= 0 } ?: return null
        val units = parts.dropLast(1).map { part -> part.toLongOrNull()?.takeIf { it >= 0 } ?: return null }
        if (units.isNotEmpty() && seconds >= BigDecimal(SECONDS_PER_UNIT)) return null
        if (units.drop(1).any { it >= SECONDS_PER_UNIT }) return null
        val wholeSeconds = units.fold(0L) { total, unit -> total * SECONDS_PER_UNIT + unit } * SECONDS_PER_UNIT
        val fraction = seconds.multiply(BigDecimal(MS_PER_SECOND)).setScale(0, RoundingMode.HALF_UP).toLong()
        return wholeSeconds * MS_PER_SECOND + fraction
    }

    /** `1:23`, or `1:23,5` when there are tenths: what the field starts with. */
    fun format(ms: Long): String {
        val tenths = (ms % MS_PER_SECOND) / MS_PER_TENTH
        val whole = VideoDuration.format(ms)
        return if (tenths == 0L) whole else "$whole$DECIMAL_COMMA$tenths"
    }
}
