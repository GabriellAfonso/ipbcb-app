package com.ipb.castelobranco.core.presentation.components.access

import app.cash.turbine.test
import com.ipb.castelobranco.core.domain.access.AccessPrompt
import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.core.domain.access.AccessStatus
import com.ipb.castelobranco.core.domain.access.MemberAccess
import com.ipb.castelobranco.core.domain.access.ObserveMemberAccessUseCase
import com.ipb.castelobranco.core.domain.access.RequestAccessUseCase
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.FakeMemberAccessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MembersOnlyViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = FakeMemberAccessRepository(
        MemberAccess(hasAccess = false, status = AccessStatus.NONE, requestsBlocked = false, correctionPending = false)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        MembersOnlyViewModel(ObserveMemberAccessUseCase(repository), RequestAccessUseCase(repository))

    @Test
    fun `the prompt follows the member access`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.uiState.launchIn(backgroundScope)

        assertEquals(AccessPrompt.CAN_REQUEST, vm.uiState.value.prompt)

        repository.memberAccess.value = repository.memberAccess.value.copy(status = AccessStatus.PENDING)
        assertEquals(AccessPrompt.PENDING, vm.uiState.value.prompt)

        repository.memberAccess.value = repository.memberAccess.value.copy(requestsBlocked = true)
        assertEquals(AccessPrompt.BLOCKED, vm.uiState.value.prompt)
    }

    @Test
    fun `sending closes the dialog, trims a blank message away and confirms`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.uiState.launchIn(backgroundScope)

        vm.messages.test {
            vm.openDialog()
            assertEquals(AccessRequestType.ACCESS, vm.uiState.value.dialog?.type)

            vm.send("   ")

            assertEquals(ACCESS_REQUEST_SENT_MESSAGE, awaitItem())
        }
        assertNull(vm.uiState.value.dialog)
        assertEquals(listOf(AccessRequestType.ACCESS to null), repository.requests)
    }

    @Test
    fun `a refusal keeps the dialog open with the server message`() = runTest(testDispatcher) {
        repository.requestResult = Result.failure(AppError.Auth(code = 403, userMessage = "Fale com a liderança."))
        val vm = viewModel()
        vm.uiState.launchIn(backgroundScope)

        vm.openDialog()
        vm.send("Sou membro.")

        val dialog = vm.uiState.value.dialog
        assertEquals("Fale com a liderança.", dialog?.error)
        assertFalse(dialog?.isSending ?: true)
    }
}
