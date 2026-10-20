package com.ipb.castelobranco.features.studies.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.studies.domain.model.DoctrineKind
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSummary
import com.ipb.castelobranco.features.studies.domain.usecase.ObserveDoctrineListUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DoctrineListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeDoctrineRepository()

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads on open and shows the documents`() = runTest(dispatcher) {
        val vm = DoctrineListViewModel(ObserveDoctrineListUseCase(repository))
        assertTrue(vm.uiState.value.isLoading)

        repository.list.value = SnapshotState.Data(
            listOf(DoctrineSummary("s", "Breve Catecismo de Westminster", "Breve Catecismo", DoctrineKind.CATECHISM, 107)),
        )
        advanceUntilIdle()

        assertEquals(1, repository.listLoads)
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Breve Catecismo", vm.uiState.value.documents.single().shortTitle)
    }

    @Test
    fun `error without a copy shows a message and retry refreshes`() = runTest(dispatcher) {
        val vm = DoctrineListViewModel(ObserveDoctrineListUseCase(repository))
        repository.list.value = SnapshotState.Error(AppError.Network())
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)

        vm.retry()
        advanceUntilIdle()
        assertEquals(1, repository.listRefreshes)
    }
}
