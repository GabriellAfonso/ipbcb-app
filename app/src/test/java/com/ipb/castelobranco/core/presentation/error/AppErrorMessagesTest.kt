package com.ipb.castelobranco.core.presentation.error

import com.ipb.castelobranco.core.domain.error.AppError
import org.junit.Assert.assertEquals
import org.junit.Test

class AppErrorMessagesTest {

    // region generic messages per category

    @Test
    fun `toUserMessage returns the connectivity text for Network`() {
        val error = AppError.Network(message = "java.net.UnknownHostException")

        assertEquals("Sem conexão com a internet. Verifique sua rede e tente novamente.", error.toUserMessage())
    }

    @Test
    fun `toUserMessage returns the login text for Auth`() {
        val error = AppError.Auth(code = 401, message = "Authentication credentials were not provided.")

        assertEquals("Faça login para continuar.", error.toUserMessage())
    }

    @Test
    fun `toUserMessage returns the permission text for a 403 without body`() {
        val error = AppError.Auth(code = 403, message = "HTTP 403")

        assertEquals("Você não tem permissão para esta ação.", error.toUserMessage())
    }

    @Test
    fun `toUserMessage keeps the backend detail of a structured 403`() {
        val error = AppError.Auth(code = 403, userMessage = "Você não tem permissão para esta ação (escopo).")

        assertEquals("Você não tem permissão para esta ação (escopo).", error.toUserMessage())
    }

    @Test
    fun `toUserMessage returns the server text for Server`() {
        val error = AppError.Server(code = 500, message = "<html><body>Server Error (500)</body></html>")

        assertEquals("Não foi possível completar a operação. Tente novamente mais tarde.", error.toUserMessage())
    }

    @Test
    fun `toUserMessage returns the fallback text for Unknown`() {
        val error = AppError.Unknown(message = "kotlin.KotlinNothingValueException")

        assertEquals("Algo deu errado. Tente novamente.", error.toUserMessage())
    }

    // endregion

    // region userMessage precedence

    @Test
    fun `toUserMessage prefers userMessage over the generic text`() {
        val error = AppError.Server(
            code = 400,
            message = """{"error_code":"chart_exists","detail":"Já existe cifra para esta música"}""",
            errorCode = "chart_exists",
            userMessage = "Já existe cifra para esta música",
        )

        assertEquals("Já existe cifra para esta música", error.toUserMessage())
    }

    @Test
    fun `toUserMessage prefers userMessage over the generic text for Auth`() {
        val error = AppError.Auth(code = 401, userMessage = "Faça login para ver a escala")

        assertEquals("Faça login para ver a escala", error.toUserMessage())
    }

    // endregion
}
