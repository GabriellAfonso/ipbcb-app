package com.ipb.castelobranco.features.admin.reports.hymnal.presentation.state

import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.EvolutionSeries
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.CalendarMonth
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.Highlight
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.HymnRanking
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.HymnalCoverage
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.InVsOutside
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportEmptyReason
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportPeriod
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportReading
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportSlice
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ServiceBulletin
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.HymnProfile
import java.time.LocalDate

/**
 * Everything the report screen renders. All of it is produced by the domain — the composables
 * pick fields, they do not compute proportions, deltas or sentences.
 *
 * @param rangeError local validation of a custom range, shown on the picker, never as a failed
 *   request.
 * @param coverage loaded lazily, the first time the coverage reading is opened, because it needs
 *   two sources the rest of the report does not use.
 */
data class HymnalReportUiState(
    val isLoading: Boolean = true,
    val error: String? = null,

    val period: ReportPeriod = ReportPeriod.ThisMonth,
    val rangeLabel: String = "",
    val slice: ReportSlice = ReportSlice.All,
    val reading: ReportReading = ReportReading.HIGHLIGHTS,
    val availableServices: List<ReportSlice.Service> = emptyList(),
    val rangeError: String? = null,

    val highlights: List<Highlight> = emptyList(),
    val ranking: HymnRanking? = null,
    val rankingExpanded: Boolean = false,
    val evolution: EvolutionSeries? = null,
    val bulletins: List<ServiceBulletin> = emptyList(),
    val calendar: List<CalendarMonth> = emptyList(),
    val inVsOutside: InVsOutside? = null,
    val emptyReason: ReportEmptyReason? = null,

    val coverage: HymnalCoverage? = null,
    val isCoverageLoading: Boolean = false,
    val coverageError: String? = null,
    val coverageEmptyReason: ReportEmptyReason? = null,

    /** The day whose bulletin the calendar reading was asked to open, if any. */
    val focusedDay: LocalDate? = null,

    /** Filled when a hymn card is open; the card screen renders it. */
    val hymnProfile: HymnProfile? = null,
    val isHymnProfileLoading: Boolean = false,
    val hymnProfileError: String? = null,

    /** "Parâmetros de coleta" is offered only to `owner` on `reports.hymnal_history`. */
    val canOpenSettings: Boolean = false,
)

/** One-shot outcomes, per the project's convention for events that must not survive rotation. */
sealed interface HymnalReportEvent {
    data class ShowMessage(val message: String) : HymnalReportEvent

    /** The report load was refused (403): leave the reports area. */
    data class LeaveArea(val message: String) : HymnalReportEvent
}
