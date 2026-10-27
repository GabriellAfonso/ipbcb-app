package com.ipb.castelobranco.features.studies.domain.usecase

import com.ipb.castelobranco.features.studies.domain.model.EditorLocation
import com.ipb.castelobranco.features.studies.domain.model.LessonBlock
import com.ipb.castelobranco.features.studies.domain.model.LessonDraft
import javax.inject.Inject

/** What is wrong with a draft before it is sent, by form field and by editor location. */
data class DraftProblems(
    val fields: Map<String, String> = emptyMap(),
    val locations: Map<EditorLocation, List<String>> = emptyMap(),
) {
    val isEmpty: Boolean get() = fields.isEmpty() && locations.isEmpty()
}

/**
 * The checks the device can make (`specs/020-sunday-school/data-model.md`); the server checks again, including the
 * Bible references.
 */
class ValidateLessonDraftUseCase @Inject constructor() {

    operator fun invoke(draft: LessonDraft): DraftProblems {
        val fields = buildMap {
            if (draft.title.isBlank()) put(FIELD_TITLE, "Informe o título")
            if (draft.year !in LessonDraft.MIN_YEAR..LessonDraft.MAX_YEAR) {
                put(FIELD_YEAR, "Ano entre ${LessonDraft.MIN_YEAR} e ${LessonDraft.MAX_YEAR}")
            }
        }
        val locations = mutableMapOf<EditorLocation, MutableList<String>>()
        if (draft.content.sections.isEmpty()) {
            locations.getOrPut(EditorLocation.Form) { mutableListOf() } += "A aula precisa de ao menos uma seção"
        }
        draft.content.sections.forEachIndexed { s, section ->
            if (section.title.isBlank()) {
                locations.getOrPut(EditorLocation.Section(s)) { mutableListOf() } += "Informe o título da seção"
            }
            if (section.blocks.isEmpty()) {
                locations.getOrPut(EditorLocation.Section(s)) { mutableListOf() } += "A seção precisa de um bloco"
            }
            section.blocks.forEachIndexed { b, block ->
                val problems = blockProblems(block)
                if (problems.isNotEmpty()) {
                    locations.getOrPut(EditorLocation.Block(s, b)) { mutableListOf() } += problems
                }
            }
        }
        return DraftProblems(fields = fields, locations = locations)
    }

    private fun blockProblems(block: LessonBlock): List<String> = buildList {
        when (block) {
            is LessonBlock.Paragraph -> {
                if (block.text.isBlank()) add("Escreva o parágrafo ou apague-o")
                if (block.proofTexts.any { it.printed.isBlank() }) add(EMPTY_REFERENCE)
            }
            is LessonBlock.BulletList -> {
                if (block.items.isEmpty()) add("A lista precisa de ao menos um item")
                if (block.items.size > MAX_ITEMS) add("A lista aceita até $MAX_ITEMS itens")
                if (block.items.any { it.text.isBlank() }) add("Há item vazio na lista")
                if (block.items.any { item -> item.proofTexts.any { it.printed.isBlank() } }) add(EMPTY_REFERENCE)
            }
            is LessonBlock.KeyVerse -> {
                if (block.text.isBlank()) add("Escreva o texto do versículo")
                if (block.proof.printed.isBlank()) add("Informe a referência do versículo")
            }
            else -> Unit
        }
        val tooLong = when (block) {
            is LessonBlock.Paragraph -> block.text.length > MAX_TEXT
            is LessonBlock.BulletList -> block.items.any { it.text.length > MAX_TEXT }
            is LessonBlock.KeyVerse -> block.text.length > MAX_TEXT
            else -> false
        }
        if (tooLong) add("Texto com mais de $MAX_TEXT caracteres")
    }

    companion object {
        const val FIELD_TITLE = "title"
        const val FIELD_YEAR = "year"
        const val MAX_TEXT = 5000
        const val MAX_ITEMS = 50
        private const val EMPTY_REFERENCE = "Há referência sem texto"
    }
}
