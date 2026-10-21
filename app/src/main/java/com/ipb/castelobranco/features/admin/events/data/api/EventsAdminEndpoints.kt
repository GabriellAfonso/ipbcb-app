package com.ipb.castelobranco.features.admin.events.data.api

import com.ipb.castelobranco.core.network.ApiConstants

/** Events management API (backend `020-church-events`). The public list lives in `features/events`. */
object EventsAdminEndpoints {
    private const val ROOT = "${ApiConstants.BASE_PATH}admin/events/"

    const val EVENTS_PATH = ROOT
    const val EVENT_PATH = "${ROOT}{id}/"

    const val ID = "id"
    const val STATUS = "status"

    const val IMAGE_PART = "image"
    const val IMAGE_FILE_NAME = "event.jpg"
    const val IMAGE_MIME = "image/jpeg"
    const val TEXT_MIME = "text/plain"
}
