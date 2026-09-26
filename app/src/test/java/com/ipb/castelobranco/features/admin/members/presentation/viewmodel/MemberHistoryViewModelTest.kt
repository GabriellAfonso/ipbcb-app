package com.ipb.castelobranco.features.admin.members.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ipb.castelobranco.features.admin.members.apiError
import com.ipb.castelobranco.features.admin.members.data.api.FakeMembersAdminApi
import com.ipb.castelobranco.features.admin.members.data.dto.HistoryDto
import com.ipb.castelobranco.features.admin.members.data.repository.MembersAdminRepositoryImpl
import com.ipb.castelobranco.features.admin.members.domain.usecase.BuildHistorySentenceUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.GetMemberHistoryUseCase
import com.ipb.castelobranco.features.admin.members.historyDto
import com.ipb.castelobranco.features.admin.members.ok
import com.ipb.castelobranco.features.admin.members.presentation.navigation.MembersRoutes
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.presentation.util.formatDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MemberHistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeMembersAdminApi()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = MemberHistoryViewModel(
        SavedStateHandle(mapOf(MembersRoutes.ARG_MEMBER_ID to 12)),
        GetMemberHistoryUseCase(MembersAdminRepositoryImpl(api), BuildHistorySentenceUseCase()),
    )

    @Test
    fun `lines keep the server order with local time`() = runTest {
        api.onGetHistory = {
            ok(
                HistoryDto(
                    listOf(historyDto(40, "photo", new = "photo changed"), historyDto(39, "created", editor = null))
                )
            )
        }
        val vm = viewModel()
        advanceUntilIdle()

        val lines = vm.uiState.value.lines
        assertEquals(listOf("trocou a foto", "cadastrou o membro"), lines.map { it.text })
        assertEquals("Usuário removido", lines[1].editor)
        assertEquals(formatDateTime(Instant.parse("2026-09-25T17:05:00Z")), lines[0].time)
    }

    @Test
    fun `empty history is an empty list`() = runTest {
        api.onGetHistory = { ok(HistoryDto(emptyList())) }
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.lines.isEmpty())
    }

    @Test
    fun `network error is shown in place`() = runTest {
        api.onGetHistory = { throw IOException("offline") }
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.error!!.contains("conexão"))
    }

    @Test
    fun `a vanished member goes back to the list`() = runTest {
        api.onGetHistory = { apiError(404, "NOT_FOUND", "Not found.") }
        val vm = viewModel()

        vm.events.test {
            advanceUntilIdle()
            assertEquals(MembersEvent.MemberGone("Este membro não existe mais"), awaitItem())
        }
    }
}
