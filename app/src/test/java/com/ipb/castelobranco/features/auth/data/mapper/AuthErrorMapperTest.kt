package com.ipb.castelobranco.features.auth.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthErrorMapperTest {

    // region parseLoginError

    @Test
    fun `parseLoginError with detail key returns its value`() {
        val result = parseLoginError("""{"detail":"Credenciais inválidas"}""")
        assertEquals("Credenciais inválidas", result)
    }

    @Test
    fun `parseLoginError with message key returns its value when detail absent`() {
        val result = parseLoginError("""{"message":"Erro de autenticação"}""")
        assertEquals("Erro de autenticação", result)
    }

    @Test
    fun `parseLoginError with error key returns its value when detail and message absent`() {
        val result = parseLoginError("""{"error":"Falha no servidor"}""")
        assertEquals("Falha no servidor", result)
    }

    @Test
    fun `parseLoginError with non_field_errors array returns first element`() {
        val result = parseLoginError("""{"non_field_errors":["Usuário ou senha inválidos"]}""")
        assertEquals("Usuário ou senha inválidos", result)
    }

    @Test
    fun `parseLoginError with unknown key returns its value`() {
        val result = parseLoginError("""{"foo":"Bar"}""")
        assertEquals("Bar", result)
    }

    @Test
    fun `parseLoginError with detail as empty array returns array toString`() {
        val result = parseLoginError("""{"detail":[]}""")
        assertEquals("[]", result)
    }

    @Test
    fun `parseLoginError with detail as nested object with detail key returns nested value`() {
        val result = parseLoginError("""{"detail":{"detail":"Mensagem aninhada"}}""")
        assertEquals("Mensagem aninhada", result)
    }

    @Test
    fun `parseLoginError with plain string input returns input unchanged`() {
        val result = parseLoginError("Erro de rede")
        assertEquals("Erro de rede", result)
    }

    @Test
    fun `parseLoginError with input starting with bracket returns input unchanged`() {
        val input = """["erro"]"""
        val result = parseLoginError(input)
        assertEquals(input, result)
    }

    @Test
    fun `parseLoginError with malformed JSON returns original input`() {
        val input = "{quebrado"
        val result = parseLoginError(input)
        assertEquals(input, result)
    }

    // endregion

    // region parseRegisterError

    @Test
    fun `parseRegisterError maps username field correctly`() {
        val result = parseRegisterError("""{"username":"Este nome já está em uso."}""")
        assertEquals("Este nome já está em uso.", result.username)
        assertNull(result.general)
    }

    @Test
    fun `parseRegisterError maps first_name snake_case field correctly`() {
        val result = parseRegisterError("""{"first_name":"Nome inválido."}""")
        assertEquals("Nome inválido.", result.firstName)
    }

    @Test
    fun `parseRegisterError maps firstName camelCase field correctly`() {
        val result = parseRegisterError("""{"firstName":"Nome inválido."}""")
        assertEquals("Nome inválido.", result.firstName)
    }

    @Test
    fun `parseRegisterError maps password_confirm field correctly`() {
        val result = parseRegisterError("""{"password_confirm":"As senhas não coincidem."}""")
        assertEquals("As senhas não coincidem.", result.passwordConfirm)
    }

    @Test
    fun `parseRegisterError maps detail field to general`() {
        val result = parseRegisterError("""{"detail":"Conta desativada."}""")
        assertEquals("Conta desativada.", result.general)
    }

    @Test
    fun `parseRegisterError maps unknown field to general`() {
        val result = parseRegisterError("""{"campo_desconhecido":"Valor inesperado."}""")
        assertEquals("Valor inesperado.", result.general)
    }

    @Test
    fun `parseRegisterError concatenates multiple unknown fields into general with newline`() {
        val result = parseRegisterError("""{"foo":"Primeiro erro.","bar":"Segundo erro."}""")
        val general = result.general ?: ""
        assert(general.contains("Primeiro erro.")) { "Expected 'Primeiro erro.' in general: $general" }
        assert(general.contains("Segundo erro.")) { "Expected 'Segundo erro.' in general: $general" }
        assert(general.contains("\n")) { "Expected newline separator in general: $general" }
    }

    @Test
    fun `parseRegisterError with array value uses first element`() {
        val result = parseRegisterError("""{"username":["Já existe um usuário com este nome."]}""")
        assertEquals("Já existe um usuário com este nome.", result.username)
    }

    @Test
    fun `parseRegisterError with empty array value uses toString`() {
        val result = parseRegisterError("""{"username":[]}""")
        assertEquals("[]", result.username)
    }

    @Test
    fun `parseRegisterError with non-JSON input returns general with original message`() {
        val input = "Erro interno do servidor"
        val result = parseRegisterError(input)
        assertEquals(input, result.general)
        assertNull(result.username)
    }

    @Test
    fun `parseRegisterError with empty JSON object returns general with original message`() {
        val input = "{}"
        val result = parseRegisterError(input)
        assertEquals(input, result.general)
    }

    // endregion

    // region new API format

    @Test
    fun `parseLoginError with new API format returns detail`() {
        val result = parseLoginError("""{"error_code":"INVALID_CREDENTIALS","detail":"Usuário ou senha inválidos"}""")
        assertEquals("Usuário ou senha inválidos", result)
    }

    @Test
    fun `parseLoginError with new API format and blank detail falls back to original`() {
        val input = """{"error_code":"UNKNOWN","detail":""}"""
        val result = parseLoginError(input)
        assertEquals(input, result)
    }

    @Test
    fun `parseRegisterError with new API format VALIDATION_ERROR maps field_errors`() {
        val input = """{"error_code":"VALIDATION_ERROR","detail":"Dados inválidos","field_errors":{"username":["Nome em uso."],"password":["Muito fraca."]}}"""
        val result = parseRegisterError(input)
        assertEquals("Nome em uso.", result.username)
        assertEquals("Muito fraca.", result.password)
        assertNull(result.general)
    }

    @Test
    fun `parseRegisterError with new API format without field_errors uses detail as general`() {
        val input = """{"error_code":"THROTTLED","detail":"Muitas requisições."}"""
        val result = parseRegisterError(input)
        assertEquals("Muitas requisições.", result.general)
        assertNull(result.username)
    }

    @Test
    fun `parseRegisterError with new API format empty field_errors uses detail as general`() {
        val input = """{"error_code":"VALIDATION_ERROR","detail":"Erro geral.","field_errors":{}}"""
        val result = parseRegisterError(input)
        assertEquals("Erro geral.", result.general)
    }

    @Test
    fun `parseRegisterError with new API format maps first_name from field_errors`() {
        val input = """{"error_code":"VALIDATION_ERROR","detail":"Erro","field_errors":{"first_name":["Obrigatório."]}}"""
        val result = parseRegisterError(input)
        assertEquals("Obrigatório.", result.firstName)
    }

    // endregion
}
