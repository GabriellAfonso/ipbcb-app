package com.ipb.castelobranco.features.profile.domain.model

/** The address that recovers a forgotten password. Never a login. */
sealed interface RecoveryEmailStatus {
    /** Not known yet: nothing about it may be shown, and the notice stays away. */
    data object Unknown : RecoveryEmailStatus

    data object None : RecoveryEmailStatus

    data class Set(val address: String, val confirmed: Boolean) : RecoveryEmailStatus
}
