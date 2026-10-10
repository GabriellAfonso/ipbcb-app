package com.ipb.castelobranco.core.data.image

import android.content.Context
import coil.memory.MemoryCache

/**
 * Share of the app's memory each Coil loader may keep decoded in RAM. Coil gives every loader 25% by
 * default, and the app runs three (default, gallery previews, member photos): up to 75% of the heap in
 * bitmaps. Each loader keeps its own cache because sign-out empties them separately.
 */
object ImageMemoryBudget {
    /** Gallery originals and album covers, plus the account avatar. */
    const val DEFAULT = 0.15

    const val GALLERY_PREVIEWS = 0.10

    const val MEMBER_PHOTOS = 0.05

    fun memoryCache(context: Context, percent: Double): MemoryCache =
        MemoryCache.Builder(context).maxSizePercent(percent).build()
}
