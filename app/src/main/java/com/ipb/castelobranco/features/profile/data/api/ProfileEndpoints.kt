package com.ipb.castelobranco.features.profile.data.api

import com.ipb.castelobranco.core.network.ApiConstants

object ProfileEndpoints {
    const val ME_PROFILE_PHOTO_PATH = "${ApiConstants.BASE_PATH}me/profile/photo/"

    const val ME_PROFILE_PATH = "${ApiConstants.BASE_PATH}me/profile/"

    const val ME_PASSWORD_PATH = "${ApiConstants.BASE_PATH}me/password/"

    /** `POST`, not `DELETE`: the password or Google token travels in the body. */
    const val ME_DELETE_PATH = "${ApiConstants.BASE_PATH}me/delete/"
}