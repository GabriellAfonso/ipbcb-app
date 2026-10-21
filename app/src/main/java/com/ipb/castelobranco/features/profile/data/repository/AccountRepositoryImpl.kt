package com.ipb.castelobranco.features.profile.data.repository

import com.ipb.castelobranco.core.domain.auth.SessionTokensWriter
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.mapError
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.profile.data.api.ProfileApi
import com.ipb.castelobranco.features.profile.data.dto.ChangePasswordRequest
import com.ipb.castelobranco.features.profile.data.dto.DeleteAccountWithGoogleRequest
import com.ipb.castelobranco.features.profile.data.dto.DeleteAccountWithPasswordRequest
import com.ipb.castelobranco.features.profile.data.dto.UpdateProfileRequest
import com.ipb.castelobranco.features.profile.domain.model.AccountDeletionProof
import com.ipb.castelobranco.features.profile.domain.repository.AccountRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val api: ProfileApi,
    private val tokensWriter: SessionTokensWriter,
) : AccountRepository {

    override suspend fun updateName(name: String): Result<Unit> =
        runCatching {
            val response = api.updateMeProfile(UpdateProfileRequest(name = name))
            if (!response.isSuccessful) throw response.toAppError()
            Unit
        }.mapError()

    override suspend fun changePassword(current: String, new: String, confirm: String): Result<Unit> =
        runCatching {
            val response = api.changePassword(
                ChangePasswordRequest(
                    currentPassword = current,
                    newPassword = new,
                    newPasswordConfirm = confirm,
                )
            )
            if (!response.isSuccessful) throw response.toAppError()

            val tokens = response.body()
                ?: throw AppError.Server(code = response.code(), message = "Resposta vazia")
            if (tokens.access.isBlank() || tokens.refresh.isBlank()) {
                throw AppError.Server(code = response.code(), message = "Par de tokens incompleto")
            }

            // Antes de qualquer outra chamada: o access usado aqui já morreu no servidor.
            tokensWriter.replace(access = tokens.access, refresh = tokens.refresh)
        }.mapError()

    override suspend fun deleteAccount(proof: AccountDeletionProof): Result<Unit> =
        runCatching {
            val response = when (proof) {
                is AccountDeletionProof.Password ->
                    api.deleteAccountWithPassword(DeleteAccountWithPasswordRequest(password = proof.password))
                is AccountDeletionProof.Google ->
                    api.deleteAccountWithGoogle(DeleteAccountWithGoogleRequest(idToken = proof.idToken))
            }
            if (!response.isSuccessful) throw response.toAppError()
            Unit
        }.mapError()
}
