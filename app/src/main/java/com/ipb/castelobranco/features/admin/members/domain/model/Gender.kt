package com.ipb.castelobranco.features.admin.members.domain.model

enum class Gender(val apiCode: String, val label: String) {
    MALE("M", "Masculino"),
    FEMALE("F", "Feminino");

    companion object {
        fun fromApiCode(code: String?): Gender? = entries.firstOrNull { it.apiCode == code }
    }
}
