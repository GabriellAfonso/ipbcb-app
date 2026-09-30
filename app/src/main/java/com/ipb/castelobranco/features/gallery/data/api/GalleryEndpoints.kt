package com.ipb.castelobranco.features.gallery.data.api

import com.ipb.castelobranco.core.network.ApiConstants

object GalleryEndpoints {
    const val CHANGES = "${ApiConstants.BASE_PATH}gallery/changes/"
    const val SINCE = "since"

    const val ID = "id"
    const val ALBUMS = "${ApiConstants.BASE_PATH}albums/"
    const val ALBUM = "${ALBUMS}{$ID}/"
    const val ALBUM_ORDER = "${ALBUMS}order/"
    const val ALBUM_COVER = "${ALBUMS}{$ID}/cover/"
    const val ALBUM_PHOTOS_ORDER = "${ALBUMS}{$ID}/photos/order/"
    const val PHOTOS = "${ApiConstants.BASE_PATH}photos/"
    const val PHOTO = "${PHOTOS}{$ID}/"

    const val PART_ALBUM_ID = "album_id"
    const val PART_IMAGE = "image"
    const val PART_CLIENT_UPLOAD_ID = "client_upload_id"
}
