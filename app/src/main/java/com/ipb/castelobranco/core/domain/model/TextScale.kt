package com.ipb.castelobranco.core.domain.model

import kotlin.math.roundToInt

/**
 * The reading text size, in percent of each screen's normal size (`specs/core/spec.md` §6.7.1). The same value means
 * the same thing everywhere, whatever the screen's base size.
 */
object TextScale {
    const val DEFAULT = 100
    const val MIN = 80
    const val MAX = 180
    const val STEP = 5

    /** The number of stops between [MIN] and [MAX], as a Material slider counts them. */
    const val SLIDER_STEPS = (MAX - MIN) / STEP - 1

    /** Rounds to the nearest [STEP] inside [MIN]..[MAX]. */
    fun normalize(percent: Int): Int = normalize(percent.toFloat())

    fun normalize(percent: Float): Int = ((percent / STEP).roundToInt() * STEP).coerceIn(MIN, MAX)

    /** A size saved in sp by older versions, as a percent of [baseSp]. */
    fun fromLegacySp(sp: Float, baseSp: Float): Int = normalize(sp / baseSp * DEFAULT)

    fun sp(baseSp: Float, percent: Int): Float = baseSp * percent / DEFAULT

    fun factor(percent: Int): Float = percent / DEFAULT.toFloat()

    fun canShrink(percent: Int): Boolean = percent > MIN

    fun canGrow(percent: Int): Boolean = percent < MAX

    fun shrunk(percent: Int): Int = normalize(percent - STEP)

    fun grown(percent: Int): Int = normalize(percent + STEP)
}
