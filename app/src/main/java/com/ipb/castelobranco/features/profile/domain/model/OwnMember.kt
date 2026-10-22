package com.ipb.castelobranco.features.profile.domain.model

import java.time.LocalDate

/**
 * The person's own roll record, read-only. Sensitive: held in memory while the profile is open, never written to
 * disk and never logged.
 */
data class OwnMember(
    val role: String?,
    val ministries: List<String>,
    val birthDay: Int?,
    val birthMonth: Int?,
    val baptismDate: LocalDate?,
    /** The roll's code: `F` or `M`; anything else is unknown. */
    val gender: String?,
)
