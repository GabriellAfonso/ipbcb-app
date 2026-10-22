package com.ipb.castelobranco.features.settings.data.repository

import com.ipb.castelobranco.core.data.local.ThemePreferences
import com.ipb.castelobranco.features.settings.domain.model.ThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryImplTest {

    private lateinit var themePreferences: ThemePreferences
    private lateinit var repository: SettingsRepositoryImpl

    private val defaultThemeModeFlow = flowOf(ThemeMode.FOLLOW_SYSTEM)
    private val defaultFontSizeFlow = flowOf(22f)

    @Before
    fun setup() {
        themePreferences = mockk(relaxed = true)
        every { themePreferences.themeModeFlow } returns defaultThemeModeFlow
        every { themePreferences.hymnalFontSizeFlow } returns defaultFontSizeFlow
        repository = SettingsRepositoryImpl(themePreferences)
    }

    // region flows

    @Test
    fun `themeModeFlow returns flow from themePreferences`() {
        assertEquals(defaultThemeModeFlow, repository.themeModeFlow)
    }

    @Test
    fun `hymnalFontSizeFlow returns flow from themePreferences`() {
        assertEquals(defaultFontSizeFlow, repository.hymnalFontSizeFlow)
    }

    // endregion

    // region setThemeMode

    @Test
    fun `setThemeMode DARK delegates to themePreferences`() = runTest {
        repository.setThemeMode(ThemeMode.DARK)
        coVerify { themePreferences.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun `setThemeMode LIGHT delegates to themePreferences`() = runTest {
        repository.setThemeMode(ThemeMode.LIGHT)
        coVerify { themePreferences.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun `setThemeMode FOLLOW_SYSTEM delegates to themePreferences`() = runTest {
        repository.setThemeMode(ThemeMode.FOLLOW_SYSTEM)
        coVerify { themePreferences.setThemeMode(ThemeMode.FOLLOW_SYSTEM) }
    }

    @Test
    fun `setThemeMode propagates exception from themePreferences`() = runTest {
        coEvery { themePreferences.setThemeMode(any()) } throws RuntimeException("disk full")
        val result = runCatching { repository.setThemeMode(ThemeMode.DARK) }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    // endregion

    // region convenience methods

    @Test
    fun `setFollowSystem delegates to themePreferences setFollowSystem`() = runTest {
        repository.setFollowSystem()
        coVerify { themePreferences.setFollowSystem() }
    }

    @Test
    fun `setLightMode delegates to themePreferences setLightMode`() = runTest {
        repository.setLightMode()
        coVerify { themePreferences.setLightMode() }
    }

    @Test
    fun `setDarkMode delegates to themePreferences setDarkMode`() = runTest {
        repository.setDarkMode()
        coVerify { themePreferences.setDarkMode() }
    }

    // endregion

    // region setHymnalFontSize

    @Test
    fun `setHymnalFontSize delegates to themePreferences`() = runTest {
        repository.setHymnalFontSize(18f)
        coVerify { themePreferences.setHymnalFontSize(18f) }
    }

    // endregion
}
