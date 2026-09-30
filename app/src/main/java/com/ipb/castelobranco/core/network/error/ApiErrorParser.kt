package com.ipb.castelobranco.core.network.error

import org.json.JSONObject
import timber.log.Timber

fun parseApiError(errorBody: String?): ApiErrorBody? {
    if (errorBody.isNullOrBlank()) return null
    return try {
        val json = JSONObject(errorBody)
        val errorCode = json.optString("error_code", "").ifBlank { return null }
        val detail = json.optString("detail", "")
        val fieldErrors = json.optJSONObject("field_errors")?.let { obj ->
            val map = mutableMapOf<String, List<String>>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = obj.optJSONArray(key)
                if (arr != null) {
                    map[key] = (0 until arr.length()).map { arr.getString(it) }
                }
            }
            map.ifEmpty { null }
        }
        val extras = buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key !in STANDARD_KEYS) put(key, json.get(key).toString())
            }
        }
        ApiErrorBody(errorCode = errorCode, detail = detail, fieldErrors = fieldErrors, extras = extras)
    } catch (e: Exception) {
        Timber.w(e, "Failed to parse API error body")
        null
    }
}

private val STANDARD_KEYS = setOf("error_code", "detail", "field_errors")
