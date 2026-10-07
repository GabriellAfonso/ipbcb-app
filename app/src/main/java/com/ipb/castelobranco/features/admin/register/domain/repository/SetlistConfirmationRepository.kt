package com.ipb.castelobranco.features.admin.register.domain.repository

import com.ipb.castelobranco.core.domain.setlist.SundaySetlist
import java.time.LocalDate

/** Setlists as the register-played side needs them. Never stored on the device. */
interface SetlistConfirmationRepository {

    /** The setlist of [date]; a missing one fails with `AppError.Server(404)`. */
    suspend fun byDate(date: LocalDate): Result<SundaySetlist>

    /** Setlists dated today or earlier whose played songs were never registered, newest first. */
    suspend fun pending(): Result<List<SundaySetlist>>

    /** Deletes the setlist of [date]; a missing one fails with `AppError.Server(404)`. */
    suspend fun delete(date: LocalDate): Result<Unit>
}
