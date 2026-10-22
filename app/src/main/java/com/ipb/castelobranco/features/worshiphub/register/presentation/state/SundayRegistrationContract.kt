package com.ipb.castelobranco.features.worshiphub.register.presentation.state

import androidx.compose.runtime.Immutable
import com.ipb.castelobranco.features.worshiphub.register.domain.validation.SundayRegistrationValidator
import com.ipb.castelobranco.core.domain.model.Song
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
) {
    val dateBr: String
        get() = selectedDate?.format(BR_DATE).orEmpty()

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
