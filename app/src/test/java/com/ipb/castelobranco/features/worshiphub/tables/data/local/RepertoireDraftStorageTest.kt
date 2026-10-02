package com.ipb.castelobranco.features.worshiphub.tables.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.DraftRow
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.RepertoireDraft
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class RepertoireDraftStorageTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher() + Job())
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var storage: RepertoireDraftStorage

    private val draft = RepertoireDraft(
        rows = listOf(
            DraftRow(1, 12, "G", true),
            DraftRow(2, null, "", false),
        ),
        updatedAtMillis = 42L,
    )

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(scope = testScope) { folder.root.resolve("draft.preferences_pb") }
        storage = RepertoireDraftStorage(dataStore, Json { ignoreUnknownKeys = true })
    }

    @Test
    fun `a saved draft is read back`() = testScope.runTest {
        storage.save(draft)

        assertEquals(draft, storage.load())
    }

    @Test
    fun `nothing saved reads null`() = testScope.runTest {
        assertNull(storage.load())
    }

    @Test
    fun `clear removes it`() = testScope.runTest {
        storage.save(draft)

        storage.clear()

        assertNull(storage.load())
    }

    @Test
    fun `an unreadable draft reads null and is removed`() = testScope.runTest {
        dataStore.edit {
            it[stringPreferencesKey("repertoire_draft_v1")] = "not json"
            it[longPreferencesKey("repertoire_draft_updated_at")] = 1L
        }

        assertNull(storage.load())
        storage.save(draft)
        assertEquals(draft, storage.load())
    }
}
