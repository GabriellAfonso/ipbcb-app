package com.ipb.castelobranco.core.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SetlistPreferencesTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var tempFile: File
    private lateinit var dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var preferences: SetlistPreferences

    private val keyDate = stringPreferencesKey("setlist_date")
    private val keySongIds = stringPreferencesKey("setlist_song_ids_v1")

    @Before
    fun setUp() {
        tempFile = File.createTempFile("setlist_prefs_test", ".preferences_pb")
            .also { it.deleteOnExit() }

        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFile }
        )
        preferences = SetlistPreferences(dataStore)
    }

    @After
    fun tearDown() {
        tempFile.delete()
    }

    @Test
    fun `pinnedSongIds returns empty list when no data stored`() = testScope.runTest {
        val result = preferences.pinnedSongIds.first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `toggleSong adds id when not present`() = testScope.runTest {
        preferences.toggleSong(42)

        val result = preferences.pinnedSongIds.first()

        assertEquals(listOf(42), result)
    }

    @Test
    fun `toggleSong removes id when already present`() = testScope.runTest {
        preferences.toggleSong(42)
        preferences.toggleSong(42)

        val result = preferences.pinnedSongIds.first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `toggleSong preserves pin insertion order`() = testScope.runTest {
        preferences.toggleSong(3)
        preferences.toggleSong(1)
        preferences.toggleSong(2)

        val result = preferences.pinnedSongIds.first()

        assertEquals(listOf(3, 1, 2), result)
    }

    @Test
    fun `toggleSong re-pin appends id at the end`() = testScope.runTest {
        preferences.toggleSong(1)
        preferences.toggleSong(2)
        preferences.toggleSong(3)
        preferences.toggleSong(2) // unpin
        preferences.toggleSong(2) // re-pin

        val result = preferences.pinnedSongIds.first()

        assertEquals(listOf(1, 3, 2), result)
    }

    @Test
    fun `pinnedSongIds returns empty list when stored date differs from today`() = testScope.runTest {
        dataStore.edit { prefs ->
            prefs[keyDate] = "2000-01-01"
            prefs[keySongIds] = "1,2,3"
        }

        val result = preferences.pinnedSongIds.first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `toggleSong on stale date resets data and adds the new id`() = testScope.runTest {
        dataStore.edit { prefs ->
            prefs[keyDate] = "2000-01-01"
            prefs[keySongIds] = "1,2"
        }

        preferences.toggleSong(99)

        assertEquals(listOf(99), preferences.pinnedSongIds.first())
    }
}
