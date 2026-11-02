package com.ipb.castelobranco.features.auth.data.api

import com.ipb.castelobranco.core.network.ApiConstants

object AuthEndpoints {
    const val AUTH_LOGIN_PATH = "${ApiConstants.BASE_PATH}auth/login/"
    const val GOOGLE_AUTH_LOGIN = "${ApiConstants.BASE_PATH}auth/google/"
    const val AUTH_REGISTER_PATH = "${ApiConstants.BASE_PATH}auth/register/"
    const val AUTH_REFRESH_PATH = "${ApiConstants.BASE_PATH}auth/refresh/"
    const val PASSWORD_RECOVERY_REQUEST_PATH = "${ApiConstants.BASE_PATH}auth/password-recovery/request/"
    const val PASSWORD_RECOVERY_CONFIRM_PATH = "${ApiConstants.BASE_PATH}auth/password-recovery/confirm/"
}