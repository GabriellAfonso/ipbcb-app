package com.ipb.castelobranco.core.testing

import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/** A Retrofit error response with [code] and a JSON [body], for fake APIs. */
fun <T> errorResponse(code: Int, body: String = """{"error_code":"ERROR","detail":"Falhou."}"""): Response<T> =
    Response.error(code, body.toResponseBody())
