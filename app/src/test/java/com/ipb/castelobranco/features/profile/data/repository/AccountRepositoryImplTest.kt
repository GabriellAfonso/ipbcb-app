package com.ipb.castelobranco.features.profile.data.repository

import com.ipb.castelobranco.core.domain.auth.SessionTokensWriter
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.errorResponse
import com.ipb.castelobranco.features.profile.data.api.FakeProfileApi
import com.ipb.castelobranco.features.profile.data.dto.ChangePasswordRequest
import com.ipb.castelobranco.features.profile.data.dto.ChangeRecoveryEmailRequest
import com.ipb.castelobranco.features.profile.data.dto.ConfirmRecoveryEmailRequest
import com.ipb.castelobranco.features.profile.data.dto.DeleteAccountWithGoogleRequest
import com.ipb.castelobranco.features.profile.data.dto.DeleteAccountWithPasswordRequest
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import com.ipb.castelobranco.features.profile.data.dto.SessionTokensDto
import com.ipb.castelobranco.features.profile.data.dto.UpdateProfileRequest
import com.ipb.castelobranco.features.profile.domain.model.AccountDeletionProof
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AccountRepositoryImplTest {

    private val api = FakeProfileApi()
    private val written = mutableListOf<Pair<String, String>>()
    private val writer = SessionTokensWriter { access, refresh -> written += access to refresh }
    private val repository = AccountRepositoryImpl(api, writer)

    @Test
    fun `changeRecoveryEmail sends the address and the password`() = runTest {
        api.recoveryEmailScript = { Response.success(Unit) }

        assertTrue(repository.changeRecoveryEmail("a@b.com", "secret1").isSuccess)

        assertEquals(listOf(ChangeRecoveryEmailRequest("a@b.com", "secret1")), api.recoveryEmailBodies)
    }

    @Test
    fun `changeRecoveryEmail carries a wrong password as a server error`() = runTest {
        api.recoveryEmailScript = {
            errorResponse(400, """{"error_code":"INVALID_PASSWORD","detail":"Senha incorreta."}""")
        }

        val error = repository.changeRecoveryEmail("a@b.com", "x").exceptionOrNull() as AppError.Server

        assertEquals("INVALID_PASSWORD", error.errorCode)
        assertEquals("Senha incorreta.", error.userMessage)
    }

    @Test
    fun `sendRecoveryEmailCode carries the wait of a rate limit`() = runTest {
        api.recoveryEmailScript = {
            errorResponse(
                429,
                """{"error_code":"EMAIL_CODE_RATE_LIMITED","detail":"Aguarde.","retry_after_seconds":42}""",
            )
        }

        val error = repository.sendRecoveryEmailCode().exceptionOrNull() as AppError.Server

        assertEquals("42", error.extras?.get("retry_after_seconds"))
    }

    @Test
    fun `confirmRecoveryEmail sends the code`() = runTest {
        api.recoveryEmailScript = { Response.success(Unit) }

        assertTrue(repository.confirmRecoveryEmail("123456").isSuccess)

        assertEquals(listOf(ConfirmRecoveryEmailRequest("123456")), api.recoveryCodeBodies)
    }

    @Test
    fun `updateName sends the name and succeeds`() = runTest {
        api.updateProfileScript = { Response.success(MeProfileDto(name = "Ana", isMember = true)) }

        val result = repository.updateName("  Ana  ")

        assertTrue(result.isSuccess)
        assertEquals(listOf(UpdateProfileRequest(name = "  Ana  ")), api.updateProfileBodies)
    }

    @Test
    fun `updateName carries the field error of a refused name`() = runTest {
        api.updateProfileScript = {
            errorResponse(
                400,
                """{"error_code":"VALIDATION_ERROR","detail":"Dados inválidos.",""" +
                    """"field_errors":{"name":["Este campo não pode ficar em branco."]}}""",
            )
        }

        val error = repository.updateName(" ").exceptionOrNull() as AppError.Server

        assertEquals(listOf("Este campo não pode ficar em branco."), error.fieldErrors?.get("name"))
    }

    @Test
    fun `changePassword stores the new pair before returning`() = runTest {
        api.changePasswordScript = { Response.success(SessionTokensDto(access = "new-a", refresh = "new-r")) }

        val result = repository.changePassword("old", "nova123", "nova123")

        assertTrue(result.isSuccess)
        assertEquals(listOf("new-a" to "new-r"), written)
        assertEquals(
            listOf(
                ChangePasswordRequest(currentPassword = "old", newPassword = "nova123", newPasswordConfirm = "nova123")
            ),
            api.changePasswordBodies,
        )
    }

    @Test
    fun `changePassword with a wrong password leaves the session untouched`() = runTest {
        api.changePasswordScript = {
            errorResponse(400, """{"error_code":"INVALID_PASSWORD","detail":"Senha incorreta."}""")
        }

        val error = repository.changePassword("wrong", "nova123", "nova123").exceptionOrNull()

        assertTrue(error is AppError.Server)
        assertEquals("INVALID_PASSWORD", (error as AppError.Server).errorCode)
        assertEquals("Senha incorreta.", error.userMessage)
        assertTrue(written.isEmpty())
    }

    @Test
    fun `changePassword with an incomplete pair fails and stores nothing`() = runTest {
        api.changePasswordScript = { Response.success(SessionTokensDto(access = "new-a", refresh = "")) }

        val result = repository.changePassword("old", "nova123", "nova123")

        assertTrue(result.exceptionOrNull() is AppError.Server)
        assertTrue(written.isEmpty())
    }

    @Test
    fun `changePassword without network is a network error`() = runTest {
        api.changePasswordScript = { throw IOException("offline") }

        val result = repository.changePassword("old", "nova123", "nova123")

        assertTrue(result.exceptionOrNull() is AppError.Network)
        assertTrue(written.isEmpty())
    }

    @Test
    fun `deleteAccount with a password sends only the password`() = runTest {
        api.deleteAccountScript = { Response.success(204, Unit) }

        val result = repository.deleteAccount(AccountDeletionProof.Password("segredo"))

        assertTrue(result.isSuccess)
        assertEquals(listOf(DeleteAccountWithPasswordRequest(password = "segredo")), api.deleteWithPasswordBodies)
        assertTrue(api.deleteWithGoogleBodies.isEmpty())
    }

    @Test
    fun `deleteAccount with Google sends only the id token`() = runTest {
        api.deleteAccountScript = { Response.success(204, Unit) }

        val result = repository.deleteAccount(AccountDeletionProof.Google("id-token"))

        assertTrue(result.isSuccess)
        assertEquals(listOf(DeleteAccountWithGoogleRequest(idToken = "id-token")), api.deleteWithGoogleBodies)
        assertTrue(api.deleteWithPasswordBodies.isEmpty())
    }

    @Test
    fun `deleteAccount blocked keeps the server message`() = runTest {
        api.deleteAccountScript = {
            errorResponse(
                409,
                """{"error_code":"ACCOUNT_DELETION_BLOCKED",""" +
                    """"detail":"Esta conta não pode ser excluída pelo app. Fale com a liderança da igreja."}""",
            )
        }

        val error = repository.deleteAccount(AccountDeletionProof.Password("x")).exceptionOrNull() as AppError.Server

        assertEquals("ACCOUNT_DELETION_BLOCKED", error.errorCode)
        assertEquals("Esta conta não pode ser excluída pelo app. Fale com a liderança da igreja.", error.userMessage)
    }

    @Test
    fun `deleteAccount with another Google account is a 403 with the server message`() = runTest {
        api.deleteAccountScript = {
            errorResponse(
                403,
                """{"error_code":"GOOGLE_ACCOUNT_MISMATCH","detail":"A conta Google não corresponde a esta conta."}""",
            )
        }

        val error = repository.deleteAccount(AccountDeletionProof.Google("t")).exceptionOrNull() as AppError.Auth

        assertEquals(403, error.code)
        assertEquals("A conta Google não corresponde a esta conta.", error.userMessage)
        assertNull(written.firstOrNull())
    }
}
