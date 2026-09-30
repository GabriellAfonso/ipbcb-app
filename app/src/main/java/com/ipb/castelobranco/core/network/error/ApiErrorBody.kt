package com.ipb.castelobranco.core.network.error

data class ApiErrorBody(
    val errorCode: String,
    val detail: String,
    val fieldErrors: Map<String, List<String>>? = null,
    /**
     * Every other top-level key of the body (e.g. `rejected`, `missing`, `chain`), as JSON text —
     * strings unquoted. Lets a feature read the structured details of an error without parsing the
     * body itself.
     */
    val extras: Map<String, String> = emptyMap(),
)
