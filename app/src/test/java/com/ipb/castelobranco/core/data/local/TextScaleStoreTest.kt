package com.ipb.castelobranco.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TextScaleStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: TextScaleStore

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(tmp.root, "settings.preferences_pb")
        }
        store = TextScaleStore(dataStore, key = NEW_KEY, legacySpKey = OLD_KEY, legacyBaseSp = BASE_SP)
    }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `nothing saved reads as 100 percent`() = runTest {
        assertEquals(100, store.flow.first())
    }

    @Test
    fun `an old size in sp reads converted`() = runTest {
        dataStore.edit { it[floatPreferencesKey(OLD_KEY)] = 22f }

        assertEquals(120, store.flow.first())
    }

    @Test
    fun `the first change saves the percent and drops the old sp`() = runTest {
        dataStore.edit { it[floatPreferencesKey(OLD_KEY)] = 22f }

        store.set(135)

        val prefs = dataStore.data.first()
        assertEquals(135, prefs[intPreferencesKey(NEW_KEY)])
        assertNull(prefs[floatPreferencesKey(OLD_KEY)])
        assertEquals(135, store.flow.first())
    }

    @Test
    fun `a value off the grid is saved and read normalized`() = runTest {
        dataStore.edit { it[intPreferencesKey(NEW_KEY)] = 163 }
        assertEquals(165, store.flow.first())

        store.set(999)
        assertEquals(180, dataStore.data.first()[intPreferencesKey(NEW_KEY)])
    }

    @Test
    fun `clear removes both keys`() = runTest {
        dataStore.edit {
            it[floatPreferencesKey(OLD_KEY)] = 22f
            it[intPreferencesKey(NEW_KEY)] = 140
        }

        dataStore.edit { store.clear(it) }

        assertEquals(100, store.flow.first())
    }

    private companion object {
        const val NEW_KEY = "test_text_scale"
        const val OLD_KEY = "test_font_size"
        const val BASE_SP = 18f
    }
}
