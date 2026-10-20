package com.ipb.castelobranco.features.studies.domain.model

import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

/**
 * Words that keep their capital in sentence-case confession titles and list headings
 * ("Da ceia do Senhor", "O Antigo Testamento").
 */
private val CAPITALIZED_WORDS = setOf(
    "deus", "cristo", "mediador", "espírito", "santo", "sagradas", "escrituras",
    "santíssima", "trindade", "lei", "igreja", "senhor", "antigo", "novo", "testamento",
)

private val WORD = Regex("""[\p{L}]+""")

/**
 * "DA CEIA DO SENHOR" → "Da ceia do Senhor": lowercases, capitalizes the first letter and restores
 * the capital of [CAPITALIZED_WORDS].
 */
fun String.toSentenceCase(): String {
    val lower = lowercase(PT_BR)
    val restored = WORD.replace(lower) { match ->
        if (match.value in CAPITALIZED_WORDS) match.value.capitalizeFirst() else match.value
    }
    return restored.capitalizeFirst()
}

private fun String.capitalizeFirst(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(PT_BR) else it.toString() }
