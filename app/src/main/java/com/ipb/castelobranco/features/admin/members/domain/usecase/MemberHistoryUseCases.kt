package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.features.admin.members.domain.model.Gender
import com.ipb.castelobranco.features.admin.members.domain.model.HistoryEntry
import com.ipb.castelobranco.features.admin.members.domain.model.HistoryLine
import com.ipb.castelobranco.features.admin.members.domain.repository.MembersAdminRepository
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Turns one server history entry into a Portuguese sentence. The server sends values as text
 * (names, ISO dates, `true`/`false`, gender codes); every raw code is translated here so the
 * screen never shows `M`, `true` or `2026-09-25`.
 */
class BuildHistorySentenceUseCase @Inject constructor() {

    operator fun invoke(entry: HistoryEntry): HistoryLine = HistoryLine(
        editorName = entry.editor?.name ?: UNKNOWN_EDITOR,
        text = sentenceFor(entry),
        changedAt = entry.changedAt,
    )

    private fun sentenceFor(entry: HistoryEntry): String = when (entry.field) {
        FIELD_CREATED -> "cadastrou o membro"
        FIELD_PHOTO ->
            if (entry.newValue == PHOTO_REMOVED) "removeu a foto" else "trocou a foto"
        else -> {
            val label = FIELD_LABELS[entry.field] ?: entry.field
            val old = valueText(entry.field, entry.oldValue)
            val new = valueText(entry.field, entry.newValue)
            "alterou $label de $old para $new"
        }
    }

    private fun valueText(field: String, value: String?): String {
        if (value == null) return EMPTY_VALUE
        return when (field) {
            FIELD_GENDER -> Gender.fromApiCode(value)?.label ?: value
            FIELD_IS_ACTIVE -> when (value) {
                TRUE -> "Válido"
                FALSE -> "Inválido"
                else -> value
            }
            FIELD_BIRTH_MONTH -> value.toIntOrNull()?.takeIf { it in Month.JANUARY.value..Month.DECEMBER.value }
                ?.let(::monthName) ?: value
            FIELD_BIRTH_DATE, FIELD_BAPTISM_DATE -> formatIsoDate(value)
            else -> value
        }
    }

    /**
     * Entries from before the birth date was split still say `birth_date`, and back then the
     * church wrote "year unknown" as the year 0001.
     */
    private fun formatIsoDate(value: String): String {
        val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return value
        return if (date.year == LEGACY_UNKNOWN_YEAR) date.format(DAY_MONTH) else date.format(DATE)
    }

    private fun monthName(month: Int): String =
        Month.of(month).getDisplayName(TextStyle.FULL, PT_BR).replaceFirstChar { it.titlecase(PT_BR) }

    companion object {
        const val UNKNOWN_EDITOR = "Usuário removido"
        const val EMPTY_VALUE = "vazio"
        private const val FIELD_CREATED = "created"
        private const val FIELD_PHOTO = "photo"
        private const val FIELD_GENDER = "gender"
        private const val FIELD_IS_ACTIVE = "is_active"
        private const val FIELD_BIRTH_DATE = "birth_date"
        private const val FIELD_BIRTH_DAY = "birth_day"
        private const val FIELD_BIRTH_MONTH = "birth_month"
        private const val FIELD_BIRTH_YEAR = "birth_year"
        private const val LEGACY_UNKNOWN_YEAR = 1
        private val PT_BR = Locale.forLanguageTag("pt-BR")
        private const val FIELD_BAPTISM_DATE = "baptism_date"
        private const val PHOTO_REMOVED = "photo removed"
        private const val TRUE = "true"
        private const val FALSE = "false"
        private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM")

        private val FIELD_LABELS = mapOf(
            "name" to "Nome",
            "first_name" to "Primeiro nome",
            "last_name" to "Sobrenome",
            FIELD_BIRTH_DAY to "Dia de nascimento",
            FIELD_BIRTH_MONTH to "Mês de nascimento",
            FIELD_BIRTH_YEAR to "Ano de nascimento",
            FIELD_BIRTH_DATE to "Nascimento",
            FIELD_GENDER to "Sexo",
            "status" to "Situação",
            "role" to "Cargo",
            "ministries" to "Ministérios",
            FIELD_BAPTISM_DATE to "Batismo",
            FIELD_IS_ACTIVE to "Perfil",
        )
    }
}

/** The member's history, newest first, already as sentences. */
class GetMemberHistoryUseCase @Inject constructor(
    private val repository: MembersAdminRepository,
    private val buildSentence: BuildHistorySentenceUseCase,
) {
    suspend operator fun invoke(id: Int): Result<List<HistoryLine>> =
        repository.getHistory(id).map { entries -> entries.map(buildSentence::invoke) }
}
