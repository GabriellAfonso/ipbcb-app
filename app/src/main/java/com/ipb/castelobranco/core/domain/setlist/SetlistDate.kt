package com.ipb.castelobranco.core.domain.setlist

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** The Sunday a setlist saved [today] is for: today when it is Sunday, otherwise the next Sunday. */
fun setlistDateFor(today: LocalDate): LocalDate =
    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
