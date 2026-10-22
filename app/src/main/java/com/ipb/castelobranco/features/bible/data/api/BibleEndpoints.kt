package com.ipb.castelobranco.features.bible.data.api

import com.ipb.castelobranco.core.network.ApiConstants

object BibleEndpoints {
    /** GET {baseUrl}/api/bible/{translation}/ */
    const val BIBLE_PATH = "${ApiConstants.BASE_PATH}bible/{translation}/"
}
