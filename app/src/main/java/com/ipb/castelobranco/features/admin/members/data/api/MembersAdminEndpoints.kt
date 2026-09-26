package com.ipb.castelobranco.features.admin.members.data.api

import com.ipb.castelobranco.core.network.ApiConstants

/**
 * Leader-only members API (backend `010-members-management`). Separate from `api/members/`, the
 * regular member list in `core/`, which returns ids and names only.
 */
object MembersAdminEndpoints {
    private const val ROOT = "${ApiConstants.BASE_PATH}admin/members/"

    const val MEMBERS_PATH = ROOT
    const val OPTIONS_PATH = "${ROOT}options/"
    const val MEMBER_PATH = "${ROOT}{id}/"
    const val PHOTO_PATH = "${ROOT}{id}/photo/"
    const val HISTORY_PATH = "${ROOT}{id}/history/"

    const val ID = "id"
    const val IF_NONE_MATCH = "If-None-Match"
    const val ETAG = "ETag"
    const val PHOTO_PART = "photo"
}
