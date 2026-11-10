package com.ipb.castelobranco.features.studies.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Text size shared by every Estudos reader (doctrine and EBD), kept apart from the Bible's. In percent of
 * [BASE_FONT_SIZE] (`specs/core/spec.md` §6.7.1).
 */
interface DoctrinePreferencesRepository {
    val textScaleFlow: Flow<Int>
    suspend fun setTextScale(percent: Int)

    companion object {
        /** The reading text at 100%. */
        const val BASE_FONT_SIZE: Float = 18f
    }
}
