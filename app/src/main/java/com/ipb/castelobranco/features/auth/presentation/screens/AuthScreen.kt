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
import com.ipb.castelobranco.features.auth.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val BRAND_NAME = "IPB Castelo Branco"
private const val SUBTITLE = "Entre com sua conta."
private const val GOOGLE_LABEL = "Continuar com Google"
private const val OR_LABEL = "ou com usuário e senha"
private const val USERNAME_LABEL = "Usuário"
private const val PASSWORD_LABEL = "Senha"
private const val SIGN_IN_LABEL = "Entrar"
private const val SIGN_IN_LOADING_LABEL = "Entrando…"
private const val NO_ACCOUNT_LABEL = "Ainda não tem conta?"
private const val REGISTER_LABEL = "Criar conta"

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onBackClick: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onAuthSuccess: () -> Unit = {},
) {
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val isLoginLoading by viewModel.isLoginLoading.collectAsStateWithLifecycle()
    val isGoogleLoading by viewModel.isGoogleLoading.collectAsStateWithLifecycle()
    var isCredentialPending by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            if (event is AuthViewModel.AuthEvent.LoginSuccess) onAuthSuccess()
        }
    }

    AuthContent(
        loginError = loginError,
        isLoginLoading = isLoginLoading,
        isGoogleLoading = isCredentialPending || isGoogleLoading,
        onSignIn = viewModel::singIn,
        onGoogleClick = {
            coroutineScope.launch {
                isCredentialPending = true
                val idToken = requestGoogleIdToken(context)
                isCredentialPending = false
                if (idToken != null) viewModel.signInWithGoogle(idToken)
            }
        },
        onCredentialsChange = viewModel::clearLoginError,
        onBackClick = onBackClick,
        onNavigateToRegister = onNavigateToRegister,
    )
}

@Composable
private fun AuthContent(
    loginError: String?,
    isLoginLoading: Boolean,
    isGoogleLoading: Boolean,
    onSignIn: (username: String, password: String) -> Unit,
    onGoogleClick: () -> Unit,
    onCredentialsChange: () -> Unit,
    onBackClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val isBusy = isLoginLoading || isGoogleLoading
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSignIn(username, password)
    }

    AuthLayout(title = BRAND_NAME, subtitle = SUBTITLE, bandSize = AuthBandSize.Login, onBackClick = onBackClick) {
        GoogleButton(label = GOOGLE_LABEL, isLoading = isGoogleLoading, enabled = !isBusy, onClick = onGoogleClick)
        OrDivider(label = OR_LABEL)

        loginError?.let { AuthErrorBanner(message = it) }

        TonalField(
            value = username,
            onValueChange = {
                username = it
                if (loginError != null) onCredentialsChange()
            },
            label = USERNAME_LABEL,
            leadingIcon = Icons.Filled.Person,
            isError = loginError != null && username.isBlank(),
            enabled = !isBusy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        TonalField(
            value = password,
            onValueChange = {
                password = it
                if (loginError != null) onCredentialsChange()
            },
            label = PASSWORD_LABEL,
            leadingIcon = Icons.Filled.Lock,
            isError = loginError != null && password.isBlank(),
            enabled = !isBusy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            visualTransformation =
                if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                    )
                }
            },
        )

        Button(
            onClick = submit,
            enabled = !isBusy,
            shape = PillShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(AuthButtonHeight),
        ) {
            if (isLoginLoading) {
                ButtonSpinner()
                Spacer(modifier = Modifier.width(8.dp))
                Text(SIGN_IN_LOADING_LABEL)
            } else {
                Text(SIGN_IN_LABEL)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = NO_ACCOUNT_LABEL,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onNavigateToRegister) {
                Text(REGISTER_LABEL, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthContentErrorPreview() {
    IPBCasteloBrancoTheme {
        AuthContent(
            loginError = "Usuário ou senha inválidos.",
            isLoginLoading = false,
            isGoogleLoading = false,
            onSignIn = { _, _ -> },
            onGoogleClick = {},
            onCredentialsChange = {},
            onBackClick = {},
            onNavigateToRegister = {},
        )
    }
}
