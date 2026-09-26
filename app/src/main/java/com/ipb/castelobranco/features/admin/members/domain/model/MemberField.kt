package com.ipb.castelobranco.features.admin.members.domain.model

/** The editable fields, each with the key the API uses in bodies and in `field_errors`. */
enum class MemberField(val apiKey: String) {
    NAME("name"),
    FIRST_NAME("first_name"),
    LAST_NAME("last_name"),
    BIRTH_DATE("birth_date"),
    GENDER("gender"),
    STATUS("status_id"),
    ROLE("role_id"),
    MINISTRIES("ministry_ids"),
    BAPTISM_DATE("baptism_date"),
    IS_VALID("is_active");

    companion object {
        fun fromApiKey(key: String): MemberField? = entries.firstOrNull { it.apiKey == key }
    }
}
