package com.ipb.castelobranco.features.auth.domain.usecase

import com.ipb.castelobranco.features.auth.domain.repository.AuthRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class LogoutUseCaseTest {

    private lateinit var repository: AuthRepository
    private lateinit var useCase: LogoutUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = LogoutUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository signOut`() = runTest {
        useCase()

        coVerify(exactly = 1) { repository.signOut() }
    }
}
