package com.ipb.castelobranco.features.studies.data.api

import com.ipb.castelobranco.core.network.ApiConstants

object DoctrineEndpoints {
    /** GET {baseUrl}/api/doctrine/ */
    const val DOCUMENTS_PATH = "${ApiConstants.BASE_PATH}doctrine/"

    /** GET {baseUrl}/api/doctrine/{slug}/ */
    const val DOCUMENT_PATH = "${ApiConstants.BASE_PATH}doctrine/{slug}/"
}
