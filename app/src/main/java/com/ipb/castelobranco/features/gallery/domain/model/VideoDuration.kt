package com.ipb.castelobranco.features.gallery.domain.model

/** A video's length as the grid shows it: `1:24`, `0:07`, `1:02:03` from one hour. */
object VideoDuration {
    private const val MS_PER_SECOND = 1000
    private const val SECONDS_PER_MINUTE = 60
    private const val SECONDS_PER_HOUR = 3600

    fun format(durationMs: Long): String {
        val totalSeconds = (durationMs.coerceAtLeast(0) / MS_PER_SECOND)
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }
}
