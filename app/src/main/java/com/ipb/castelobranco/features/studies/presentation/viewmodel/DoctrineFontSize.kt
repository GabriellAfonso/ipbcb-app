package com.ipb.castelobranco.features.studies.presentation.viewmodel

import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.FONT_SIZE_STEP
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.MAX_FONT_SIZE
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.MIN_FONT_SIZE

/** Text size of the doctrine readers and whether each step of the "Aa" bar is available. */
data class DoctrineFontSize(val size: Float) {
    val canDecrease: Boolean get() = size > MIN_FONT_SIZE
    val canIncrease: Boolean get() = size < MAX_FONT_SIZE

    fun increased(): Float = (size + FONT_SIZE_STEP).coerceAtMost(MAX_FONT_SIZE)
    fun decreased(): Float = (size - FONT_SIZE_STEP).coerceAtLeast(MIN_FONT_SIZE)
}
