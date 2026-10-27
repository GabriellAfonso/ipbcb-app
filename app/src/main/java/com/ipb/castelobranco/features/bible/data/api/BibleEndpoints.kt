package com.ipb.castelobranco.features.bible.data.api

import com.ipb.castelobranco.core.network.ApiConstants

object BibleEndpoints {
    /** GET {baseUrl}/api/bible/{translation}/ */
    const val BIBLE_PATH = "${ApiConstants.BASE_PATH}bible/{translation}/"

    const val ID = "id"
    const val BOOK = "book"
    const val CHAPTER = "chapter"

    /** GET (public, ETag) / POST (manage). */
    const val VIDEOS = "${ApiConstants.BASE_PATH}bible/videos/"

    /** DELETE (owner). */
    const val VIDEO = "${VIDEOS}{$ID}/"

    /** PUT the whole list / DELETE one place (`?book=&chapter=`; no `chapter` = whole book). */
    const val VIDEO_PLACES = "${VIDEOS}{$ID}/places/"

    /** PUT the order of one place. */
    const val VIDEOS_ORDER = "${VIDEOS}order/"

    /** GET the signed-in user's progress (only what is done). */
    const val PROGRESS = "${ApiConstants.BASE_PATH}bible/progress/"

    /** POST a batch of queued changes. */
    const val PROGRESS_SYNC = "${PROGRESS}sync/"
}
