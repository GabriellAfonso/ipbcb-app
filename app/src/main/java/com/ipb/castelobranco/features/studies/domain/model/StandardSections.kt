package com.ipb.castelobranco.features.studies.domain.model

/**
 * The six sections every lesson follows, with their default icons (`backend/specs/sunday-school/spec.md`). The
 * server applies no default: the app starts a new lesson from them.
 */
object StandardSections {

    val sections: List<Pair<String, LessonIcon>> = listOf(
        "Contexto no Drama das Escrituras" to LessonIcon.HOURGLASS,
        "Objetivo teológico" to LessonIcon.CROWN,
        "Estrutura / Esboço" to LessonIcon.BOOK_OPEN,
        "Verdades teológicas" to LessonIcon.SHIELD,
        "Versículos-chave" to LessonIcon.SCROLL,
        "Exortação devocional" to LessonIcon.HANDS_PRAYING,
    )

    /** Each standard section with one empty paragraph to write in. */
    fun template(): LessonContent = LessonContent(
        sections = sections.map { (title, icon) ->
            LessonSection(title = title, iconName = icon.wireName, blocks = listOf(LessonBlock.Paragraph("")))
        },
    )
}
