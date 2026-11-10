package com.ipb.castelobranco.features.studies.presentation.viewmodel

import com.ipb.castelobranco.core.domain.model.TextScale
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.BASE_FONT_SIZE

/** Text size of the Estudos readers: the bar's [percent] and the text's [size] in sp. */
data class DoctrineTextSize(val percent: Int = TextScale.DEFAULT) {
    val size: Float get() = TextScale.sp(BASE_FONT_SIZE, percent)
}
