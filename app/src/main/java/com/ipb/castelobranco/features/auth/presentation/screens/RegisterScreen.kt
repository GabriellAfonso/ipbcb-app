package com.ipb.castelobranco.features.auth.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.ipb.castelobranco.R
import com.ipb.castelobranco.features.auth.presentation.viewmodel.AuthViewModel
import com.ipb.castelobranco.features.auth.domain.model.RegisterErrors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onBackClick: () -> Unit = {},
    onAuthSuccess: () -> Unit = {},
) {
    val username = remember { mutableStateOf("") }
    val firstName = remember { mutableStateOf("") }
    val lastName = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val passwordVisible = remember { mutableStateOf(false) }
    val registerErrors by viewModel.registerErrors.collectAsState()

    LaunchedEffect(registerErrors) {
        if (registerErrors != RegisterErrors()) {
            delay(5000)
            viewModel.clearRegisterErrors()
        }
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AuthViewModel.AuthEvent.RegisterSuccess -> onAuthSuccess()
                else -> {}
            }
        }
    }

    BaseScreen(
        tabName = "Registro",
        logoRes = R.drawable.ic_auth_register,
        showBackArrow = true,
        onBackClick = onBackClick,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (registerErrors.general != null) {
                Text(
                    text = registerErrors.general!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            OutlinedTextField(
                value = username.value,
                onValueChange = { username.value = it },
                label = { Text("Nome de Usuário") },
                modifier = Modifier.fillMaxWidth(),
                isError = registerErrors.username != null,
                supportingText = {
                    registerErrors.username?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            OutlinedTextField(
                value = firstName.value,
                onValueChange = { firstName.value = it },
                label = { Text("Nome") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                isError = registerErrors.firstName != null,
                supportingText = {
                    registerErrors.firstName?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            OutlinedTextField(
                value = lastName.value,
                onValueChange = { lastName.value = it },
                label = { Text("Sobrenome") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                isError = registerErrors.lastName != null,
                supportingText = {
                    registerErrors.lastName?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Senha") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                visualTransformation =
                    if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val img = if (passwordVisible.value) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    Icon(
                        imageVector = img,
                        contentDescription = if (passwordVisible.value) "Ocultar senha" else "Mostrar senha",
                        modifier = Modifier.clickable { passwordVisible.value = !passwordVisible.value }
                    )
                },
                isError = registerErrors.password != null,
                supportingText = {
                    registerErrors.password?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            OutlinedTextField(
                value = confirmPassword.value,
                onValueChange = { confirmPassword.value = it },
                label = { Text("Confirmar Senha") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                visualTransformation =
                    if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                isError = registerErrors.passwordConfirm != null,
                supportingText = {
                    registerErrors.passwordConfirm?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            Button(
                onClick = {
                    viewModel.singUp(
                        username = username.value,
                        firstName = firstName.value,
                        lastName = lastName.value,
                        password = password.value,
                        passwordConfirm = confirmPassword.value
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Registrar")
            }

            TextButton(
                onClick = onBackClick,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Já tem uma conta? Entrar")
            }
        }
    }
}