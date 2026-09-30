package com.ipb.castelobranco.features.gallery.domain.manage

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError

/**
 * Runs [write] for each id in order and keeps going after a failure, so one bad photo never leaves
 * the rest undone. A lost access level stops it: every following photo would get the same answer,
 * and they are reported with it. One sync at the end.
 */
internal suspend fun runBatch(
    ids: List<Long>,
    repository: GalleryManageRepository,
    write: suspend (Long) -> Result<*>,
): BatchResult {
    var succeeded = 0
    val failures = mutableListOf<AppError>()
    for ((index, id) in ids.withIndex()) {
        val error = write(id).exceptionOrNull()?.toAppError()
        if (error == null) {
            succeeded++
            continue
        }
        failures += error
        if (error.isForbidden()) {
            repeat(ids.size - index - 1) { failures += error }
            break
        }
    }
    if (succeeded > 0) repository.syncAfterWrite()
    return BatchResult(succeeded, failures)
}
