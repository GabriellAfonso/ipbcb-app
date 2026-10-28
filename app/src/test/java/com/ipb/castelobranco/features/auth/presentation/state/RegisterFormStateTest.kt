package com.ipb.castelobranco.features.auth.presentation.state

import com.ipb.castelobranco.features.auth.domain.model.RegisterErrors
import com.ipb.castelobranco.features.auth.domain.model.RegisterRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterFormStateTest {

    private val valid = RegisterFormState(
        firstName = "Maria",
        lastName = "Oliveira",
        username = "maria.oliveira",
        password = "abc123",
        passwordConfirm = "abc123",
    )

    @Test
    fun `username rule matches the backend`() {
        listOf("ana", "ana.paula", "a-b_c", "a1").forEach { assertTrue(it, RegisterRules.isValidUsername(it)) }
        listOf("", ".", "..", ".ana", "ana.", "Ana", "ana paula", "joão").forEach {
            assertFalse(it, RegisterRules.isValidUsername(it))
        }
    }

    @Test
    fun `username is lowercased and loses spaces while typed`() {
        assertEquals("maria.oliveira", RegisterRules.normalizeUsername("Maria .Oliveira "))
    }

    @Test
    fun `valid form shows hints and checks`() {
        assertTrue(valid.isValid)
        assertEquals(FieldHelp(USERNAME_HINT, HelpTone.Neutral), valid.usernameHelp)
        assertEquals(FieldHelp(PASSWORD_OK, HelpTone.Ok), valid.passwordHelp)
        assertEquals(FieldHelp(CONFIRM_OK, HelpTone.Ok), valid.passwordConfirmHelp)
        assertNull(valid.firstNameHelp)
        assertNull(valid.bannerMessage)
    }

    @Test
    fun `username outside the rule is an error before submit`() {
        val form = valid.copy(username = "maria.")

        assertEquals(FieldHelp(USERNAME_RULE_MESSAGE, HelpTone.Error), form.usernameHelp)
        assertFalse(form.isValid)
    }

    @Test
    fun `short confirmation stays quiet until it is as long as the password`() {
        assertNull(valid.copy(passwordConfirm = "abc").passwordConfirmHelp)
        assertEquals(CONFIRM_MISMATCH_MESSAGE, valid.copy(passwordConfirm = "abc124").passwordConfirmHelp?.text)
    }

    @Test
    fun `after submit empty fields and short password are errors`() {
        val form = RegisterFormState(password = "abc", submitted = true)

        assertEquals(REQUIRED_MESSAGE, form.firstNameHelp?.text)
        assertEquals(REQUIRED_MESSAGE, form.lastNameHelp?.text)
        assertEquals(REQUIRED_MESSAGE, form.usernameHelp.text)
        assertEquals(PASSWORD_SHORT_MESSAGE, form.passwordHelp.text)
        assertEquals(REQUIRED_MESSAGE, form.passwordConfirmHelp?.text)
    }

    @Test
    fun `server errors win over hints and general goes to the banner`() {
        val form = valid.copy(
            serverErrors = RegisterErrors(username = "Este nome de usuário já está em uso.", general = "Falhou."),
        )

        assertEquals(FieldHelp("Este nome de usuário já está em uso.", HelpTone.Error), form.usernameHelp)
        assertEquals("Falhou.", form.bannerMessage)
    }

    @Test
    fun `refused submit shows the fix banner`() {
        assertEquals(FIX_FIELDS_MESSAGE, RegisterFormState(showFixBanner = true).bannerMessage)
    }
}
