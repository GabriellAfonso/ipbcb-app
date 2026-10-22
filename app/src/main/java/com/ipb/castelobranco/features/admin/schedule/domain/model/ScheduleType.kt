package com.ipb.castelobranco.features.admin.schedule.domain.model

enum class ScheduleType(val displayName: String, val id: Int) {
    TUESDAY_PRAYER("Terça de Oração", 1),
    THURSDAY_PRAYER("Quinta de Oração", 2),
    SUNDAY_LITURGY("Domingo Liturgia de Adoração", 3);

    companion object {
        fun idByName(name: String): Int =
            entries.firstOrNull { it.displayName == name }?.id ?: 0
    }
}
