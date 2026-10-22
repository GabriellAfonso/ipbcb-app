package com.ipb.castelobranco.core.presentation.navigation

import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Where a tapped notification takes the user. Carried as `CoreActivity` intent extras. */
sealed interface NotificationTarget {

    /** "Repertório de domingo dd/MM disponível": the lyrics list, with the Sunday section on top. */
    data object SundaySetlist : NotificationTarget

    /** "Confirmar músicas de domingo": the register screen pre-filled with the setlist of [date]. */
    data class ConfirmPlays(val date: LocalDate) : NotificationTarget

    /** "Novo pedido de acesso": Gestão › Perfis, on Pendentes. */
    data object ProfilesPending : NotificationTarget

    /** The requester's request was decided: their profile, with the access card. */
    data object Profile : NotificationTarget

    companion object {
        const val EXTRA_TARGET = "com.ipb.castelobranco.extra.TARGET"
        const val EXTRA_DATE = "com.ipb.castelobranco.extra.DATE"
        const val TARGET_SUNDAY_SETLIST = "sunday_setlist"
        const val TARGET_CONFIRM_PLAYS = "confirm_plays"
        const val TARGET_PROFILES_PENDING = "profiles_pending"
        const val TARGET_PROFILE = "profile"

        /** `null` for a normal launch or extras that do not make a valid target. */
        fun fromExtras(target: String?, date: String?): NotificationTarget? = when (target) {
            TARGET_SUNDAY_SETLIST -> SundaySetlist
            TARGET_CONFIRM_PLAYS -> date.toLocalDateOrNull()?.let { ConfirmPlays(it) }
            TARGET_PROFILES_PENDING -> ProfilesPending
            TARGET_PROFILE -> Profile
            else -> null
        }

        private fun String?.toLocalDateOrNull(): LocalDate? = this?.let {
            try {
                LocalDate.parse(it)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
