package com.ipb.castelobranco.features.auth.presentation.state

import com.ipb.castelobranco.features.auth.domain.model.RegisterErrors
import com.ipb.castelobranco.features.auth.domain.model.RegisterRules

const val REQUIRED_MESSAGE = "Preencha este campo."
const val USERNAME_HINT = "Letras minúsculas, números, ponto, hífen ou _."
const val USERNAME_RULE_MESSAGE =
    "Use só letras minúsculas, números, ponto, hífen ou _, começando e terminando com letra ou número."
const val PASSWORD_HINT = "Mínimo de 6 caracteres."
const val PASSWORD_OK = "✓ Mínimo de 6 caracteres."
const val PASSWORD_SHORT_MESSAGE = "A senha precisa de pelo menos 6 caracteres."
const val CONFIRM_OK = "✓ As senhas coincidem."
const val CONFIRM_MISMATCH_MESSAGE = "As senhas não coincidem."
const val FIX_FIELDS_MESSAGE = "Confira os campos marcados."

enum class HelpTone { Neutral, Ok, Error }

/** The line under a field: a hint, a check that passed, or an error. */
data class FieldHelp(val text: String, val tone: HelpTone) {
    val isError: Boolean get() = tone == HelpTone.Error
}

/**
 * The register form (`specs/auth/spec.md` §4). [submitted] turns on after the first tap on "Criar conta": from then
 * on, empty fields are errors too. [serverErrors] holds the backend's answer until the field is edited.
 */
data class RegisterFormState(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val submitted: Boolean = false,
    val showFixBanner: Boolean = false,
    val serverErrors: RegisterErrors = RegisterErrors(),
    val isLoading: Boolean = false,
) {
    val firstNameHelp: FieldHelp? get() = nameHelp(firstName, serverErrors.firstName)
    val lastNameHelp: FieldHelp? get() = nameHelp(lastName, serverErrors.lastName)

    val usernameHelp: FieldHelp
        get() = when {
            serverErrors.username != null -> error(serverErrors.username)
            username.isEmpty() -> if (submitted) error(REQUIRED_MESSAGE) else FieldHelp(USERNAME_HINT, HelpTone.Neutral)
            !RegisterRules.isValidUsername(username) -> error(USERNAME_RULE_MESSAGE)
            else -> FieldHelp(USERNAME_HINT, HelpTone.Neutral)
        }

    val passwordHelp: FieldHelp
        get() = when {
            serverErrors.password != null -> error(serverErrors.password)
            passwordLongEnough -> FieldHelp(PASSWORD_OK, HelpTone.Ok)
            submitted -> error(PASSWORD_SHORT_MESSAGE)
            else -> FieldHelp(PASSWORD_HINT, HelpTone.Neutral)
        }

    /** Nothing while empty; a mismatch shows once the confirmation is as long as the password, or after a submit. */
    val passwordConfirmHelp: FieldHelp?
        get() = when {
            serverErrors.passwordConfirm != null -> error(serverErrors.passwordConfirm)
            passwordConfirm.isEmpty() -> if (submitted) error(REQUIRED_MESSAGE) else null
            passwordConfirm == password -> FieldHelp(CONFIRM_OK, HelpTone.Ok)
            submitted || passwordConfirm.length >= password.length -> error(CONFIRM_MISMATCH_MESSAGE)
            else -> null
        }

    /** The backend's message without a field, or the reminder after a submit the screen refused. */
    val bannerMessage: String?
        get() = serverErrors.general ?: FIX_FIELDS_MESSAGE.takeIf { showFixBanner }

    /** What the screen checks before calling the backend. */
    val isValid: Boolean
        get() = firstName.isNotBlank() && lastName.isNotBlank() && RegisterRules.isValidUsername(username) &&
            passwordLongEnough && passwordConfirm == password

    private val passwordLongEnough: Boolean get() = password.length >= RegisterRules.PASSWORD_MIN_LENGTH

    private fun nameHelp(value: String, serverError: String?): FieldHelp? = when {
        serverError != null -> error(serverError)
        submitted && value.isBlank() -> error(REQUIRED_MESSAGE)
        else -> null
    }

    private fun error(text: String) = FieldHelp(text, HelpTone.Error)
}
