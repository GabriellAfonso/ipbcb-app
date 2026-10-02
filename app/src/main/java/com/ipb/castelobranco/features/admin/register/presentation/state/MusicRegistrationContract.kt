package com.ipb.castelobranco.features.admin.register.presentation.state

import androidx.compose.runtime.Immutable
import com.ipb.castelobranco.features.admin.register.domain.validation.MusicRegistrationValidator
import com.ipb.castelobranco.core.domain.model.Song
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class RegistrationType(val label: String) {
    MUSIC("Registrar música"),
    SUNDAY("Registrar domingo")
}

@Immutable
data class SundaySongRowState(
    val position: Int,
    val songQuery: String = "",
    val selectedSongId: Int? = null,
    val tone: String = ""
)

@Immutable
data class MusicSongFormState(
    val title: String = "",
    val artist: String = "",
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
data class MusicRegistrationUiState(
    val registrationType: RegistrationType = RegistrationType.SUNDAY,

    // Sunday
    val availableSongs: List<Song> = emptyList(),
    val isLoadingSongs: Boolean = false,
    val selectedDate: LocalDate? = LocalDate.now(),
    val showDatePicker: Boolean = false,
    val sundayRows: List<SundaySongRowState> = defaultSundayRows(),
    val sundayRowErrors: Map<Int, String> = emptyMap(),

    // Music
    val musicForm: MusicSongFormState = MusicSongFormState(),

    val isSubmitting: Boolean = false,
    val snackbarMessage: String? = null,

    /** The Sunday being confirmed; fixes the date. `null` for a register opened from the panel. */
    val prefillDate: LocalDate? = null,
    val prefill: PrefillState = PrefillState.None,
) {
    val dateBr: String
        get() = if (registrationType == RegistrationType.SUNDAY) selectedDate?.format(BR_DATE).orEmpty() else ""

    val canSubmit: Boolean
        get() = when (registrationType) {
            RegistrationType.SUNDAY -> {
                val dateOk = selectedDate != null
                val validation = MusicRegistrationValidator.validateSundayRows(sundayRows, availableSongs)
                dateOk && validation.incompletePositions.isEmpty() && validation.hasAtLeastOneCompleteValidRow
            }
            RegistrationType.MUSIC -> {
                musicForm.title.isNotBlank() && musicForm.artist.isNotBlank()
            }
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

sealed interface MusicRegistrationEvent {
    data object Init : MusicRegistrationEvent
    data class RegistrationTypeChanged(val type: RegistrationType) : MusicRegistrationEvent

    // Sunday
    data object OpenDatePicker : MusicRegistrationEvent
    data object DismissDatePicker : MusicRegistrationEvent
    data class DatePicked(val date: LocalDate) : MusicRegistrationEvent
    data class SundaySongSelected(val position: Int, val song: Song) : MusicRegistrationEvent
    data class SundayToneChanged(val position: Int, val tone: String) : MusicRegistrationEvent
    data object AddSundayRow : MusicRegistrationEvent
    data class RemoveSundayRow(val position: Int) : MusicRegistrationEvent

    // Music
    data class MusicTitleChanged(val title: String) : MusicRegistrationEvent
    data class MusicArtistChanged(val artist: String) : MusicRegistrationEvent

    data object Submit : MusicRegistrationEvent
    data object SnackbarShown : MusicRegistrationEvent
    data object RetryPrefill : MusicRegistrationEvent
}