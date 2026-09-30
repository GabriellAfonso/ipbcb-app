package com.ipb.castelobranco.core.network.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ApiErrorParserTest {

    @Test
    fun `parses new API format with error_code and detail`() {
        val body = """{"error_code":"NOT_FOUND","detail":"Recurso não encontrado"}"""
        val result = parseApiError(body)
        assertNotNull(result)
        assertEquals("NOT_FOUND", result!!.errorCode)
        assertEquals("Recurso não encontrado", result.detail)
        assertNull(result.fieldErrors)
    }

    @Test
    fun `parses new API format with field_errors`() {
        val body = """{"error_code":"VALIDATION_ERROR","detail":"Dados inválidos","field_errors":{"username":["Já existe.","Muito curto."],"password":["Fraca."]}}"""
        val result = parseApiError(body)
        assertNotNull(result)
        assertEquals("VALIDATION_ERROR", result!!.errorCode)
        assertEquals("Dados inválidos", result.detail)
        assertNotNull(result.fieldErrors)
        assertEquals(listOf("Já existe.", "Muito curto."), result.fieldErrors!!["username"])
        assertEquals(listOf("Fraca."), result.fieldErrors!!["password"])
    }

    @Test
    fun `parses throttled error`() {
        val body = """{"error_code":"THROTTLED","detail":"Muitas requisições. Tente novamente em 30 segundos."}"""
        val result = parseApiError(body)
        assertNotNull(result)
        assertEquals("THROTTLED", result!!.errorCode)
        assertEquals("Muitas requisições. Tente novamente em 30 segundos.", result.detail)
    }

    @Test
    fun `returns null for null input`() {
        assertNull(parseApiError(null))
    }

    @Test
    fun `returns null for blank input`() {
        assertNull(parseApiError(""))
        assertNull(parseApiError("   "))
    }

    @Test
    fun `returns null for legacy format without error_code`() {
        assertNull(parseApiError("""{"detail":"Credenciais inválidas"}"""))
    }

    @Test
    fun `returns null for legacy format with error key`() {
        assertNull(parseApiError("""{"error":"Falha no servidor"}"""))
    }

    @Test
    fun `returns null for malformed JSON`() {
        assertNull(parseApiError("{quebrado"))
    }

    @Test
    fun `returns null for plain text`() {
        assertNull(parseApiError("Erro de rede"))
    }

    @Test
    fun `returns null when error_code is blank`() {
        assertNull(parseApiError("""{"error_code":"","detail":"msg"}"""))
    }

    @Test
    fun `field_errors is null when object is empty`() {
        val body = """{"error_code":"VALIDATION_ERROR","detail":"Erro","field_errors":{}}"""
        val result = parseApiError(body)
        assertNotNull(result)
        assertNull(result!!.fieldErrors)
    }

    // region extras

    @Test
    fun `extras carry every non-standard top-level key as JSON text, strings unquoted`() {
        val body = """{"error_code":"VALIDATION_ERROR","detail":"Nenhuma imagem foi aceita.",""" +
            """"rejected":[{"filename":"a.jpg","reason":"Grande demais."}],"missing":[4],"unexpected":[8],""" +
            """"repeated":[],"chain":[9,5,3],"client_upload_id":"abc"}"""

        val extras = parseApiError(body)!!.extras

        val rejected = org.json.JSONArray(extras["rejected"]).getJSONObject(0)
        assertEquals("a.jpg", rejected.getString("filename"))
        assertEquals("Grande demais.", rejected.getString("reason"))
        assertEquals("[4]", extras["missing"])
        assertEquals("[8]", extras["unexpected"])
        assertEquals("[]", extras["repeated"])
        assertEquals("[9,5,3]", extras["chain"])
        assertEquals("abc", extras["client_upload_id"])
    }

    @Test
    fun `extras never repeat error_code, detail or field_errors`() {
        val body = """{"error_code":"VALIDATION_ERROR","detail":"x","field_errors":{"name":["y"]},"chain":[1]}"""

        val extras = parseApiError(body)!!.extras

        assertEquals(setOf("chain"), extras.keys)
    }

    @Test
    fun `a body without extras yields an empty map`() {
        val body = """{"error_code":"NOT_FOUND","detail":"Recurso não encontrado"}"""

        assertEquals(emptyMap<String, String>(), parseApiError(body)!!.extras)
    }

    // endregion
}
