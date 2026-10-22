package com.ipb.castelobranco.features.admin.schedule.data.api

import com.ipb.castelobranco.core.network.ApiConstants

object AdminScheduleEndpoints {
    const val MEMBERS  = "${ApiConstants.BASE_PATH}members/"
    const val GENERATE = "${ApiConstants.BASE_PATH}schedule/generate/"
    const val SAVE     = "${ApiConstants.BASE_PATH}schedule/save/"
}