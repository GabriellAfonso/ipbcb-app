package com.ipb.castelobranco.features.gallery.domain.tags

import java.text.Normalizer
import java.util.Locale

/** Name search that ignores case and accents: "joao" finds "João". */
object NameSearch {

    private val MARKS = Regex("\\p{Mn}+")

    fun normalize(text: String): String =
        MARKS.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase(Locale.ROOT)

    /** A blank [query] matches every name. */
    fun matches(name: String, query: String): Boolean {
        val needle = normalize(query.trim())
        return needle.isEmpty() || normalize(name).contains(needle)
    }
}
