package com.ipb.castelobranco.features.events.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import coil.ImageLoader
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.features.events.domain.usecase.GetEventUseCase
import com.ipb.castelobranco.features.events.testing.FakeEventsRepository
import com.ipb.castelobranco.features.events.testing.event
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 11, 10)
    private val repository = FakeEventsRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(id: Int) = EventDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(AppRoutes.ARG_EVENT_ID to id)),
        getEvent = GetEventUseCase(repository, DateProvider { today }),
        imageLoader = mockk<ImageLoader>(),
    )

    @Test
    fun `a saved event shows its rows, absent ones stay null`() = runTest(dispatcher) {
        repository.state.value = SnapshotState.Data(
            listOf(
                event(3, start = LocalDate.of(2026, 11, 14), end = LocalDate.of(2026, 11, 16), location = "Templo")
            )
        )

        val state = viewModel(3).uiState.first { !it.isLoading }

        val event = requireNotNull(state.event)
        assertEquals("14 a 16 de novembro", event.dateLabel)
        assertEquals("Templo", event.location)
        assertNull(event.timeLabel)
        assertNull(event.description)
    }

    @Test
    fun `tags, duration and the shared text are worded for the poster layout`() = runTest(dispatcher) {
        repository.state.value = SnapshotState.Data(
            listOf(
                event(
                    3,
                    start = LocalDate.of(2026, 11, 14),
                    end = LocalDate.of(2026, 11, 16),
                    time = LocalTime.of(19, 30),
                    title = "Congresso de Jovens",
                    location = "Templo",
                    description = "Traga um amigo!",
                ),
                event(4, start = LocalDate.of(2026, 11, 14), title = "Ceia"),
            )
        )

        val event = requireNotNull(viewModel(3).uiState.first { !it.isLoading }.event)
        assertEquals("14 a 16 nov", event.shortDateLabel)
        assertEquals("3 dias", event.durationLabel)
        assertEquals(
            "*Congresso de Jovens*\n\n📅 14 a 16 de novembro\n⏰ 19:30\n📍 Templo\n\nTraga um amigo!",
            event.shareText,
        )

        val oneDay = requireNotNull(viewModel(4).uiState.first { !it.isLoading }.event)
        assertNull(oneDay.durationLabel)
        assertEquals("*Ceia*\n\n📅 Sábado, 14 de novembro", oneDay.shareText)
    }

    @Test
    fun `an event no longer active is gone`() = runTest(dispatcher) {
        repository.state.value = SnapshotState.Data(listOf(event(1, start = today.minusDays(3))))

        val state = viewModel(1).uiState.first { !it.isLoading }

        assertTrue(state.isGone)
        assertNull(state.event)
    }
}
