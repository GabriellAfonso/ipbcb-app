package com.ipb.castelobranco.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.ipb.castelobranco.core.domain.model.TextScale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * One area's reading text size, saved in percent (`specs/core/spec.md` §6.7.1). Until the first change, a size an
 * older version saved in sp under [legacySpKey] is read converted against [legacyBaseSp]; the first change writes
 * [key] and drops the old one.
 */
class TextScaleStore(
    private val dataStore: DataStore<Preferences>,
    key: String,
    legacySpKey: String? = null,
    private val legacyBaseSp: Float = 0f,
) {
    private val percentKey = intPreferencesKey(key)
    private val legacyKey = legacySpKey?.let(::floatPreferencesKey)

    val flow: Flow<Int> = dataStore.data.map(::read)

    suspend fun set(percent: Int) {
        dataStore.edit { prefs ->
            prefs[percentKey] = TextScale.normalize(percent)
            removeLegacy(prefs)
        }
    }

    /** Removes the size inside an edit the owner is already making. */
    fun clear(prefs: MutablePreferences) {
        prefs.remove(percentKey)
        removeLegacy(prefs)
    }

    // A statement, never `let(prefs::remove)`: `remove` returns the old value as a non-null Float, and a missing key
    // would unbox null and crash.
    private fun removeLegacy(prefs: MutablePreferences) {
        val key = legacyKey ?: return
        if (key in prefs) prefs.remove(key)
    }

    private fun read(prefs: Preferences): Int {
        prefs[percentKey]?.let { return TextScale.normalize(it) }
        val legacy = legacyKey?.let { prefs[it] } ?: return TextScale.DEFAULT
        return TextScale.fromLegacySp(legacy, legacyBaseSp)
    }
}
