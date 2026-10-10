package com.ipb.castelobranco.core.domain.util

private val combiningMarks = Regex("\\p{InCombiningDiacriticalMarks}+")
private val punctuation = Regex("[\\p{Punct}]+")

fun String.normalize(): String =
    java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .replace(punctuation, "")
