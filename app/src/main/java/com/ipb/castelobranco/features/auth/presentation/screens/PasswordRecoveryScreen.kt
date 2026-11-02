package com.ipb.castelobranco.features.auth.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.features.auth.presentation.components.AuthBandSize
import com.ipb.castelobranco.features.auth.presentation.components.AuthButtonHeight
import com.ipb.castelobranco.features.auth.presentation.components.AuthErrorBanner
import com.ipb.castelobranco.features.auth.presentation.components.AuthLayout
import com.ipb.castelobranco.features.auth.presentation.components.ButtonSpinner
import com.ipb.castelobranco.features.auth.presentation.components.PillShape
import com.ipb.castelobranco.features.auth.presentation.components.TonalField
import com.ipb.castelobranco.features.auth.presentation.state.PasswordRecoveryState
import com.ipb.castelobranco.features.auth.presentation.state.RecoveryStep
import com.ipb.castelobranco.features.auth.presentation.viewmodel.PasswordRecoveryViewModel

private const val TITLE = "Recuperar senha"
private const val SUBTITLE = "IPB Castelo Branco"
private const val EMAIL_LABEL = "E-mail"
private const val SEND_LABEL = "Enviar código"
private const val SEND_LOADING_LABEL = "Enviando…"
private const val GOOGLE_HINT = "Se você entra pelo Google, use o botão do Google."
private const val EMAIL_STEP_TEXT = "Digite o e-mail da sua conta para receber um código."
private const val SENT_TEXT = "Se houver conta com esse e-mail, enviamos um código."
private const val CODE_EXPLANATION =
    "O e-mail mostra o nome de usuário. Se houver mais de uma conta nesse e-mail, cada uma tem seu código: " +
        "digite o da conta que quer recuperar."
private const val CODE_LABEL = "Código"
private const val NEW_PASSWORD_LABEL = "Nova senha"
private const val CONFIRM_LABEL = "Confirmar nova senha"
private const val RESET_LABEL = "Trocar senha"
private const val RESET_LOADING_LABEL = "Trocando senha…"
private const val RESEND_LABEL = "Reenviar código"

/** "Esqueci minha senha" (`specs/auth/spec.md` §5): the e-mail, then the code and the new password. */
@Composable
fun PasswordRecoveryScreen(
    onBackClick: () -> Unit,
    onPasswordReset: () -> Unit,
    viewModel: PasswordRecoveryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnPasswordReset by rememberUpdatedState(onPasswordReset)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PasswordRecoveryViewModel.Event.PasswordReset -> currentOnPasswordReset()
            }
        }
    }

    val goBack = { if (!viewModel.goBackOneStep() && !state.isBusy) onBackClick() }
    BackHandler(onBack = goBack)

    PasswordRecoveryContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onSendCode = viewModel::sendCode,
        onCodeChange = viewModel::onCodeChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onNewPasswordConfirmChange = viewModel::onNewPasswordConfirmChange,
        onResetPassword = viewModel::resetPassword,
        onResendCode = viewModel::resendCode,
        onBackClick = goBack,
    )
}

@Composable
private fun PasswordRecoveryContent(
    state: PasswordRecoveryState,
    onEmailChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onCodeChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onNewPasswordConfirmChange: (String) -> Unit,
    onResetPassword: () -> Unit,
    onResendCode: () -> Unit,
    onBackClick: () -> Unit,
) {
    AuthLayout(title = TITLE, subtitle = SUBTITLE, bandSize = AuthBandSize.Register, onBackClick = onBackClick) {
        when (state.step) {
            RecoveryStep.Email -> EmailStep(state, onEmailChange, onSendCode)
            RecoveryStep.Code -> CodeStep(
                state = state,
                onCodeChange = onCodeChange,
                onNewPasswordChange = onNewPasswordChange,
                onNewPasswordConfirmChange = onNewPasswordConfirmChange,
                onResetPassword = onResetPassword,
                onResendCode = onResendCode,
            )
        }
    }
}

@Composable
private fun EmailStep(
    state: PasswordRecoveryState,
    onEmailChange: (String) -> Unit,
    onSendCode: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSendCode()
    }

    ExplanationText(EMAIL_STEP_TEXT)
    state.banner?.let { AuthErrorBanner(message = it) }
    TonalField(
        value = state.email,
        onValueChange = onEmailChange,
        label = EMAIL_LABEL,
        leadingIcon = Icons.Filled.Email,
        isError = false,
        enabled = !state.isBusy,
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        help = state.emailHelp,
    )
    PrimaryButton(
        label = SEND_LABEL,
        loadingLabel = SEND_LOADING_LABEL,
        isLoading = state.isSending,
        enabled = !state.isBusy,
        onClick = submit,
    )
    ExplanationText(GOOGLE_HINT)
}

@Composable
private fun CodeStep(
    state: PasswordRecoveryState,
    onCodeChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onNewPasswordConfirmChange: (String) -> Unit,
    onResetPassword: () -> Unit,
    onResendCode: () -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onResetPassword()
    }
    val transformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
    val passwordOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)

    Text(text = SENT_TEXT, style = MaterialTheme.typography.bodyMedium)
    ExplanationText(CODE_EXPLANATION)
    state.banner?.let { AuthErrorBanner(message = it) }
    TonalField(
        value = state.code,
        onValueChange = onCodeChange,
        label = CODE_LABEL,
        leadingIcon = Icons.Filled.Pin,
        isError = false,
        enabled = !state.isBusy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
        help = state.codeHelp,
    )
    TonalField(
        value = state.newPassword,
        onValueChange = onNewPasswordChange,
        label = NEW_PASSWORD_LABEL,
        leadingIcon = Icons.Filled.Lock,
        isError = false,
        enabled = !state.isBusy,
        keyboardOptions = passwordOptions,
        visualTransformation = transformation,
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                )
            }
        },
        help = state.newPasswordHelp,
    )
    TonalField(
        value = state.newPasswordConfirm,
        onValueChange = onNewPasswordConfirmChange,
        label = CONFIRM_LABEL,
        leadingIcon = Icons.Filled.Lock,
        isError = false,
        enabled = !state.isBusy,
        keyboardOptions = passwordOptions.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        visualTransformation = transformation,
        help = state.newPasswordConfirmHelp,
    )
    PrimaryButton(
        label = RESET_LABEL,
        loadingLabel = RESET_LOADING_LABEL,
        isLoading = state.isResetting,
        enabled = !state.isBusy,
        onClick = submit,
    )
    TextButton(
        onClick = onResendCode,
        enabled = state.canResend,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (state.resendSecondsLeft > 0) "$RESEND_LABEL (${state.resendSecondsLeft}s)" else RESEND_LABEL)
    }
}

@Composable
private fun ExplanationText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PrimaryButton(
    label: String,
    loadingLabel: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(AuthButtonHeight),
    ) {
        if (isLoading) {
            ButtonSpinner()
            Spacer(modifier = Modifier.width(8.dp))
            Text(loadingLabel)
        } else {
            Text(label)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordRecoveryCodeStepPreview() {
    IPBCasteloBrancoTheme {
        PasswordRecoveryContent(
            state = PasswordRecoveryState(
                step = RecoveryStep.Code,
                email = "maria@example.com",
                code = "123456",
                banner = "Código inválido ou expirado.",
                resendSecondsLeft = 42,
            ),
            onEmailChange = {},
            onSendCode = {},
            onCodeChange = {},
            onNewPasswordChange = {},
            onNewPasswordConfirmChange = {},
            onResetPassword = {},
            onResendCode = {},
            onBackClick = {},
        )
    }
}
