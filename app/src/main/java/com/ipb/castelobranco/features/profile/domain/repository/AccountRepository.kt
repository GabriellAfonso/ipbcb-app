package com.ipb.castelobranco.features.profile.domain.repository

import com.ipb.castelobranco.features.profile.domain.model.AccountDeletionProof

/** Account writes. Failures are `AppError`s; field errors ride on `AppError.Server.fieldErrors`. */
interface AccountRepository {

    /** Sends the name as given; the server trims it and refuses blank or more than 100 characters. */
    suspend fun updateName(name: String): Result<Unit>

    /**
     * Changes the password. On success the new session pair is already stored when this returns: the token used
     * for the call is dead, so nothing may run with it first.
     */
    suspend fun changePassword(current: String, new: String, confirm: String): Result<Unit>

    /** Deletes the account on the server. Clearing the local session is the caller's job. */
    suspend fun deleteAccount(proof: AccountDeletionProof): Result<Unit>
}
