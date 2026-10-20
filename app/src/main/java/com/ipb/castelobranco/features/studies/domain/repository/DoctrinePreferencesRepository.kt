package com.ipb.castelobranco.features.studies.domain.repository

import kotlinx.coroutines.flow.Flow

/** Text size shared by every doctrine reader, kept apart from the Bible's. */
interface DoctrinePreferencesRepository {
    val fontSizeFlow: Flow<Float>
    suspend fun setFontSize(size: Float)

    companion object {
        const val DEFAULT_FONT_SIZE: Float = 18f
        const val MIN_FONT_SIZE: Float = 14f
        const val MAX_FONT_SIZE: Float = 32f
        const val FONT_SIZE_STEP: Float = 2f
    }
}
