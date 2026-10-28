package com.ipb.castelobranco.features.auth.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.auth.requestGoogleIdToken
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.features.auth.presentation.components.AuthBandSize
import com.ipb.castelobranco.features.auth.presentation.components.AuthButtonHeight
import com.ipb.castelobranco.features.auth.presentation.components.AuthErrorBanner
import com.ipb.castelobranco.features.auth.presentation.components.AuthLayout
import com.ipb.castelobranco.features.auth.presentation.components.ButtonSpinner
import com.ipb.castelobranco.features.auth.presentation.components.GoogleButton
import com.ipb.castelobranco.features.auth.presentation.components.OrDivider
import com.ipb.castelobranco.features.auth.presentation.components.PillShape
import com.ipb.castelobranco.features.auth.presentation.components.TonalField
import com.ipb.castelobranco.features.auth.presentation.state.RegisterFormState
import com.ipb.castelobranco.features.auth.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TITLE = "Criar conta"
private const val SUBTITLE = "IPB Castelo Branco"
private const val GOOGLE_LABEL = "Criar com Google"
private const val OR_LABEL = "ou preencha seus dados"
private const val FIRST_NAME_LABEL = "Nome"
private const val LAST_NAME_LABEL = "Sobrenome"
private const val USERNAME_LABEL = "Usuário"
private const val PASSWORD_LABEL = "Senha"
private const val CONFIRM_LABEL = "Confirmar senha"
private const val SUBMIT_LABEL = "Criar conta"
private const val SUBMIT_LOADING_LABEL = "Criando conta…"
private const val HAS_ACCOUNT_LABEL = "Já tem conta?"
private const val SIGN_IN_LABEL = "Entrar"

private val NameGap = 10.dp

/** Register screen, "tudo numa folha" (`specs/auth/spec.md` §4). */
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onBackClick: () -> Unit = {},
    onAuthSuccess: () -> Unit = {},
) {
    val form by viewModel.registerForm.collectAsStateWithLifecycle()
    val googleError by viewModel.loginError.collectAsStateWithLifecycle()
    val isGoogleLoading by viewModel.isGoogleLoading.collectAsStateWithLifecycle()
    var isCredentialPending by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // An account made with Google signs in like the login does.
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                AuthViewModel.AuthEvent.RegisterSuccess, AuthViewModel.AuthEvent.LoginSuccess -> onAuthSuccess()
            }
        }
    }

    RegisterContent(
        form = form,
        googleError = googleError,
        isGoogleLoading = isCredentialPending || isGoogleLoading,
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onPasswordConfirmChange = viewModel::onPasswordConfirmChange,
        onSubmit = viewModel::singUp,
        onGoogleClick = {
            coroutineScope.launch {
                isCredentialPending = true
                val idToken = requestGoogleIdToken(context)
                isCredentialPending = false
                if (idToken != null) viewModel.signInWithGoogle(idToken)
            }
        },
        onBackClick = onBackClick,
    )
}

@Composable
private fun RegisterContent(
    form: RegisterFormState,
    googleError: String?,
    isGoogleLoading: Boolean,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoogleClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val isBusy = form.isLoading || isGoogleLoading
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSubmit()
    }
    val passwordTransformation =
        if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
    val next = KeyboardOptions(imeAction = ImeAction.Next)

    AuthLayout(title = TITLE, subtitle = SUBTITLE, bandSize = AuthBandSize.Register, onBackClick = onBackClick) {
        GoogleButton(label = GOOGLE_LABEL, isLoading = isGoogleLoading, enabled = !isBusy, onClick = onGoogleClick)
        OrDivider(label = OR_LABEL)

        (googleError ?: form.bannerMessage)?.let { AuthErrorBanner(message = it) }

        Row(horizontalArrangement = Arrangement.spacedBy(NameGap), modifier = Modifier.fillMaxWidth()) {
            TonalField(
                value = form.firstName,
                onValueChange = onFirstNameChange,
                label = FIRST_NAME_LABEL,
                leadingIcon = null,
                isError = false,
                enabled = !isBusy,
                keyboardOptions = next.copy(capitalization = KeyboardCapitalization.Words),
                help = form.firstNameHelp,
                modifier = Modifier.weight(1f),
            )
            TonalField(
                value = form.lastName,
                onValueChange = onLastNameChange,
                label = LAST_NAME_LABEL,
                leadingIcon = null,
                isError = false,
                enabled = !isBusy,
                keyboardOptions = next.copy(capitalization = KeyboardCapitalization.Words),
                help = form.lastNameHelp,
                modifier = Modifier.weight(1f),
            )
        }
        TonalField(
            value = form.username,
            onValueChange = onUsernameChange,
            label = USERNAME_LABEL,
            leadingIcon = Icons.Filled.Person,
            isError = false,
            enabled = !isBusy,
            keyboardOptions = next.copy(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii),
            help = form.usernameHelp,
        )
        TonalField(
            value = form.password,
            onValueChange = onPasswordChange,
            label = PASSWORD_LABEL,
            leadingIcon = Icons.Filled.Lock,
            isError = false,
            enabled = !isBusy,
            keyboardOptions = next.copy(keyboardType = KeyboardType.Password),
            visualTransformation = passwordTransformation,
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                    )
                }
            },
            help = form.passwordHelp,
        )
        TonalField(
            value = form.passwordConfirm,
            onValueChange = onPasswordConfirmChange,
            label = CONFIRM_LABEL,
            leadingIcon = Icons.Filled.Lock,
            isError = false,
            enabled = !isBusy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            visualTransformation = passwordTransformation,
            help = form.passwordConfirmHelp,
        )

        Button(
            onClick = submit,
            enabled = !isBusy,
            shape = PillShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(AuthButtonHeight),
        ) {
            if (form.isLoading) {
                ButtonSpinner()
                Spacer(modifier = Modifier.width(8.dp))
                Text(SUBMIT_LOADING_LABEL)
            } else {
                Text(SUBMIT_LABEL)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = HAS_ACCOUNT_LABEL,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onBackClick) {
                Text(SIGN_IN_LABEL, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterContentPreview() {
    IPBCasteloBrancoTheme {
        RegisterContent(
            form = RegisterFormState(
                firstName = "Maria",
                lastName = "Oliveira",
                username = "maria oliveira",
                password = "abc123",
                passwordConfirm = "abc12",
            ),
            googleError = null,
            isGoogleLoading = false,
            onFirstNameChange = {},
            onLastNameChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onPasswordConfirmChange = {},
            onSubmit = {},
            onGoogleClick = {},
            onBackClick = {},
        )
    }
}
