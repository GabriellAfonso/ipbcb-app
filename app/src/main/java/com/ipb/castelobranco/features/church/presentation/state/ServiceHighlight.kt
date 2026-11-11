package com.ipb.castelobranco.features.church.presentation.state

import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/** How long after its start a service still counts as happening now. */
val LIVE_WINDOW: Duration = Duration.ofMinutes(90)

enum class ServiceBadge { LIVE, NEXT }

data class ServiceHighlight(val service: ChurchService, val badge: ServiceBadge)

/**
 * The one service that gets a badge at [now]: the one that started less than [liveWindow] ago, or else the next one to
 * start, even if that is next week. Null only for an empty list.
 */
fun List<ChurchService>.highlightAt(now: LocalDateTime, liveWindow: Duration = LIVE_WINDOW): ServiceHighlight? {
    var live: Pair<ChurchService, LocalDateTime>? = null
    var next: Pair<ChurchService, LocalDateTime>? = null
    for (service in this) {
        val thisWeek = now.toLocalDate().with(TemporalAdjusters.previousOrSame(service.day)).atTime(service.time)
        val lastStart = if (thisWeek.isAfter(now)) thisWeek.minusWeeks(1) else thisWeek
        val nextStart = if (thisWeek.isAfter(now)) thisWeek else thisWeek.plusWeeks(1)
        if (now.isBefore(lastStart.plus(liveWindow)) && (live == null || lastStart.isAfter(live.second))) {
            live = service to lastStart
        }
        if (next == null || nextStart.isBefore(next.second)) next = service to nextStart
    }
    return live?.let { ServiceHighlight(it.first, ServiceBadge.LIVE) }
        ?: next?.let { ServiceHighlight(it.first, ServiceBadge.NEXT) }
}
