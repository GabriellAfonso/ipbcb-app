package com.ipb.castelobranco.features.admin.reports.hymnal.presentation.util

import com.ipb.castelobranco.core.domain.error.AppError

private const val HTTP_FORBIDDEN = 403

/**
 * The backend refused the user on this scope. On a load it takes the user out of the reports area;
 * on a save it is only a message (spec 006, FR-025/FR-026).
 */
fun AppError.isPermissionRefusal(): Boolean = this is AppError.Auth && code == HTTP_FORBIDDEN
