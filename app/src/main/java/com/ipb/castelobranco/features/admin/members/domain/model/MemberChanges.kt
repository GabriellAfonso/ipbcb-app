package com.ipb.castelobranco.features.admin.members.domain.model

import java.time.LocalDate

/**
 * The fields to send, and nothing else. A present key with a null value means "clear it"; an
 * absent key means "leave it alone" — the difference a PATCH depends on.
 *
 * Value types by field: names are [String]; dates [LocalDate]; [MemberField.GENDER] a [Gender];
 * [MemberField.STATUS]/[MemberField.ROLE] an [Int] id; [MemberField.MINISTRIES] a `Set<Int>`;
 * [MemberField.IS_VALID] a [Boolean].
 */
data class MemberChanges(val values: Map<MemberField, Any?>) {
    val isEmpty: Boolean get() = values.isEmpty()
}

/**
 * What must be sent to turn [original] into this draft. With no [original] (a new member) every
 * filled field goes, plus validity only when it is not the server default (valid).
 */
fun MemberDraft.changesFrom(original: MemberDraft?): MemberChanges {
    val current = fieldValues()
    if (original == null) {
        val filled = current.filter { (field, value) ->
            when (field) {
                MemberField.NAME -> true
                MemberField.IS_VALID -> value == false
                else -> value.isFilled()
            }
        }
        return MemberChanges(filled)
    }
    val before = original.fieldValues()
    return MemberChanges(current.filter { (field, value) -> before[field] != value })
}

private fun MemberDraft.fieldValues(): Map<MemberField, Any?> = linkedMapOf(
    MemberField.NAME to name.trim(),
    MemberField.FIRST_NAME to firstName.trim(),
    MemberField.LAST_NAME to lastName.trim(),
    MemberField.BIRTH_DATE to birthDate,
    MemberField.GENDER to gender,
    MemberField.STATUS to statusId,
    MemberField.ROLE to roleId,
    MemberField.MINISTRIES to ministryIds,
    MemberField.BAPTISM_DATE to baptismDate,
    MemberField.IS_VALID to isValid,
)

private fun Any?.isFilled(): Boolean = when (this) {
    null -> false
    is String -> isNotBlank()
    is Collection<*> -> isNotEmpty()
    else -> true
}
