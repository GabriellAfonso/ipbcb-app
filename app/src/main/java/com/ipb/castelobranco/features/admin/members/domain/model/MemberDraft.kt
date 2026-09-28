package com.ipb.castelobranco.features.admin.members.domain.model

import java.time.LocalDate

/** The form's working copy. [id] null means a new member. */
data class MemberDraft(
    val id: Int? = null,
    val name: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val birth: BirthDate = BirthDate.NONE,
    val gender: Gender? = null,
    val statusId: Int? = null,
    val roleId: Int? = null,
    val ministryIds: Set<Int> = emptySet(),
    val baptismDate: LocalDate? = null,
    val isValid: Boolean = true,
)
