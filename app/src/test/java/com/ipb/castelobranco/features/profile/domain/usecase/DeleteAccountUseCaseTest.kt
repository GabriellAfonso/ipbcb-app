package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.core.domain.auth.AuthEventBus
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.profile.domain.model.AccountDeletionProof
import com.ipb.castelobranco.features.profile.domain.model.isDeletionConfirmationTyped
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteAccountUseCaseTest {

    private val repository = FakeAccountRepository()
    private val bus = RecordingAuthEventBus()
    private val useCase = DeleteAccountUseCase(repository, bus)

    @Test
    fun `a deletion announces that the session must end`() = runTest {
        val result = useCase(AccountDeletionProof.Password("segredo"))

        assertTrue(result.isSuccess)
        assertEquals(listOf(AccountDeletionProof.Password("segredo")), repository.deletions)
        assertEquals(listOf<AuthEventBus.Event>(AuthEventBus.Event.AccountDeleted), bus.emitted)
    }

    @Test
    fun `a refused deletion keeps the session`() = runTest {
        repository.deleteResult = Result.failure(AppError.Server(code = 400, errorCode = "INVALID_PASSWORD"))

        val result = useCase(AccountDeletionProof.Google("token"))

        assertTrue(result.isFailure)
        assertTrue(bus.emitted.isEmpty())
    }

    @Test
    fun `the confirmation word ignores case and surrounding spaces only`() {
        assertTrue(isDeletionConfirmationTyped(" excluir "))
        assertTrue(isDeletionConfirmationTyped("EXCLUIR"))
        assertFalse(isDeletionConfirmationTyped("EXCLUI"))
        assertFalse(isDeletionConfirmationTyped("EX CLUIR"))
    }
}
