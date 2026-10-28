package com.ipb.castelobranco.features.worshiphub.register.presentation.state

import androidx.compose.runtime.Immutable
import com.ipb.castelobranco.features.worshiphub.register.domain.validation.SundayRegistrationValidator
import com.ipb.castelobranco.core.domain.model.Song
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
data class SundaySongRowState(
    val position: Int,
    val songQuery: String = "",
    val selectedSongId: Int? = null,
    val tone: String = ""
)

/** Reading the setlist that pre-fills a Sunday opened from a notification or the pending card. */
sealed interface PrefillState {
    data object None : PrefillState
    data object Loading : PrefillState
    data object Loaded : PrefillState
    data object NotFound : PrefillState
    data class Failed(val message: String) : PrefillState
}

@Immutable
data class SundayRegistrationUiState(
    val availableSongs: List<Song> = emptyList(),
    val isLoadingSongs: Boolean = false,
    val selectedDate: LocalDate? = LocalDate.now(),
    val showDatePicker: Boolean = false,
    val sundayRows: List<SundaySongRowState> = defaultSundayRows(),
    val sundayRowErrors: Map<Int, String> = emptyMap(),

    val isSubmitting: Boolean = false,
    val snackbarMessage: String? = null,

    /** The Sunday being confirmed; fixes the date. `null` for a register opened from the Tables menu. */
    val prefillDate: LocalDate? = null,
    val prefill: PrefillState = PrefillState.None,
    /** Who saved the setlist that filled the rows, when known. */
    val prefillSavedBy: String? = null,
) {
    val dateBr: String
        get() = selectedDate?.format(BR_DATE).orEmpty()

    /** "Domingo, 25/10/2026". */
    val dateTitle: String
        get() = selectedDate?.let { date ->
            date.format(WEEKDAY_DATE).replaceFirstChar { it.titlecase(PT_BR) }
        }.orEmpty()

    /** What still blocks "Registrar domingo", or `null` when it can be sent. */
    val missingText: String?
        get() {
            val validation = SundayRegistrationValidator.validateSundayRows(sundayRows, availableSongs)
            val incomplete = validation.incompletePositions.sorted()
            return when {
                selectedDate == null -> "escolha a data"
                incomplete.size == 1 -> "complete música e tom da posição ${incomplete.single()}"
                incomplete.size > 1 -> "complete música e tom das posições " +
                    "${incomplete.dropLast(1).joinToString(", ")} e ${incomplete.last()}"
                !validation.hasAtLeastOneCompleteValidRow -> "escolha ao menos uma música"
                else -> null
            }
        }

    /** The rows that will be sent, as the confirmation lists them: "1. Agnus Dei · A". */
    val confirmLines: List<String>
        get() = sundayRows.filter { it.selectedSongId != null && it.tone.isNotBlank() }
            .mapIndexed { index, row -> "${index + 1}. ${songOf(row)?.title ?: row.songQuery} · ${row.tone}" }

    /** The row's song: from the catalog, or from the setlist label while the catalog is loading. */
    fun songOf(row: SundaySongRowState): Song? {
        val id = row.selectedSongId ?: return null
        return availableSongs.firstOrNull { it.id == id } ?: Song(
            id = id,
            title = row.songQuery.substringBefore(ARTIST_OPEN),
            artist = row.songQuery.substringAfter(ARTIST_OPEN, "").removeSuffix(ARTIST_CLOSE),
            categoryName = "",
        )
    }

    val canSubmit: Boolean
        get() {
            val validation = SundayRegistrationValidator.validateSundayRows(sundayRows, availableSongs)
            return selectedDate != null &&
                validation.incompletePositions.isEmpty() &&
                validation.hasAtLeastOneCompleteValidRow
        }

    companion object {
        val BR_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
        private val WEEKDAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", PT_BR)
        /** `SongLabelFormatter`: "Title [Artist]". */
        private const val ARTIST_OPEN = " ["
        private const val ARTIST_CLOSE = "]"

        fun defaultSundayRows(): List<SundaySongRowState> = listOf(
            SundaySongRowState(position = 1),
            SundaySongRowState(position = 2),
            SundaySongRowState(position = 3),
            SundaySongRowState(position = 4),
        )
    }
}

sealed interface SundayRegistrationEvent {
    data object Init : SundayRegistrationEvent

    data object OpenDatePicker : SundayRegistrationEvent
    data object DismissDatePicker : SundayRegistrationEvent
    data class DatePicked(val date: LocalDate) : SundayRegistrationEvent
    data class SundaySongSelected(val position: Int, val song: Song) : SundayRegistrationEvent
    data class SundayToneChanged(val position: Int, val tone: String) : SundayRegistrationEvent
    data object AddSundayRow : SundayRegistrationEvent
    data class RemoveSundayRow(val position: Int) : SundayRegistrationEvent

    data object Submit : SundayRegistrationEvent
    data object SnackbarShown : SundayRegistrationEvent
    data object RetryPrefill : SundayRegistrationEvent
}
