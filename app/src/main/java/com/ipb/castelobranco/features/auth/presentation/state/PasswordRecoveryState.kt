package com.ipb.castelobranco.features.auth.presentation.state

import com.ipb.castelobranco.features.auth.domain.model.EmailRules
import com.ipb.castelobranco.features.auth.domain.model.RegisterRules

const val RECOVERY_CODE_LENGTH = 6
const val RECOVERY_CODE_MESSAGE = "Digite os 6 dígitos do código."

enum class RecoveryStep { Email, Code }

/**
 * The recovery flow (`specs/auth/spec.md` §5). Lives in the view model only, never in saved state: it holds a new
 * password. [submitted] turns on after the first tap on the step's button: from then on, wrong fields are errors.
 */
data class PasswordRecoveryState(
    val step: RecoveryStep = RecoveryStep.Email,
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val newPasswordConfirm: String = "",
    val submitted: Boolean = false,
    val serverEmailError: String? = null,
    val serverPasswordError: String? = null,
    val serverConfirmError: String? = null,
    val banner: String? = null,
    val isSending: Boolean = false,
    val isResetting: Boolean = false,
    val resendSecondsLeft: Int = 0,
) {
    val trimmedEmail: String get() = email.trim()

    val isBusy: Boolean get() = isSending || isResetting
    val canResend: Boolean get() = resendSecondsLeft == 0 && !isBusy

    val emailHelp: FieldHelp?
        get() = when {
            serverEmailError != null -> error(serverEmailError)
            !submitted -> null
            trimmedEmail.isEmpty() -> error(REQUIRED_MESSAGE)
            !EmailRules.isValid(trimmedEmail) -> error(EMAIL_RULE_MESSAGE)
            else -> null
        }

    val codeHelp: FieldHelp?
        get() = if (submitted && !codeComplete) error(RECOVERY_CODE_MESSAGE) else null

    val newPasswordHelp: FieldHelp
        get() = when {
            serverPasswordError != null -> error(serverPasswordError)
            passwordLongEnough -> FieldHelp(PASSWORD_OK, HelpTone.Ok)
            submitted -> error(PASSWORD_SHORT_MESSAGE)
            else -> FieldHelp(PASSWORD_HINT, HelpTone.Neutral)
        }

    val newPasswordConfirmHelp: FieldHelp?
        get() = when {
            serverConfirmError != null -> error(serverConfirmError)
            newPasswordConfirm.isEmpty() -> if (submitted) error(REQUIRED_MESSAGE) else null
            newPasswordConfirm == newPassword -> FieldHelp(CONFIRM_OK, HelpTone.Ok)
            submitted || newPasswordConfirm.length >= newPassword.length -> error(CONFIRM_MISMATCH_MESSAGE)
            else -> null
        }

    val isEmailValid: Boolean get() = EmailRules.isValid(trimmedEmail)

    val isCodeStepValid: Boolean
        get() = codeComplete && passwordLongEnough && newPasswordConfirm == newPassword

    private val codeComplete: Boolean get() = code.length == RECOVERY_CODE_LENGTH
    private val passwordLongEnough: Boolean get() = newPassword.length >= RegisterRules.PASSWORD_MIN_LENGTH

    private fun error(text: String) = FieldHelp(text, HelpTone.Error)
}
