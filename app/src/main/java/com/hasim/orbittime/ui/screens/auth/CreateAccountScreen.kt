package com.hasim.orbittime.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.data.auth.AuthValidation
import com.hasim.orbittime.ui.components.AuthScreenScaffold
import com.hasim.orbittime.ui.components.OrbitGradientButton
import com.hasim.orbittime.ui.components.OrbitTextField
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

@Composable
fun CreateAccountScreen(
    onBackClick: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onAccountCreated: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    CreateAccountContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onNavigateToSignIn = onNavigateToSignIn,
        onCreateAccountClick = { name, email, password ->
            viewModel.createAccount(name, email, password, onAccountCreated)
        },
    )
}

@Composable
fun CreateAccountContent(
    uiState: AuthUiState,
    onBackClick: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onCreateAccountClick: (name: String, email: String, password: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    AuthScreenScaffold(
        headline = "Create account",
        subtitle = "Start tracking your time in a few taps.",
        onBackClick = onBackClick,
        footer = {
            Row {
                Text(
                    text = "Already have an account? ",
                    style = OrbitTypography.bodyMedium,
                    color = OrbitColors.slate600,
                )
                Text(
                    text = "Sign in",
                    style = OrbitTypography.titleMedium,
                    color = OrbitColors.violet600,
                    modifier = Modifier.clickable(onClick = onNavigateToSignIn),
                )
            }
        },
    ) {
        OrbitTextField(
            label = "FULL NAME",
            value = name,
            onValueChange = {
                name = it
                nameError = null
            },
            errorText = nameError,
        )
        Spacer(modifier = Modifier.height(OrbitSpacing.lg))
        OrbitTextField(
            label = "WORK EMAIL",
            value = email,
            onValueChange = {
                email = it
                emailError = null
            },
            keyboardType = KeyboardType.Email,
            errorText = emailError,
        )
        Spacer(modifier = Modifier.height(OrbitSpacing.lg))
        OrbitTextField(
            label = "PASSWORD",
            value = password,
            onValueChange = {
                password = it
                passwordError = null
                if (confirmPassword.isNotEmpty()) {
                    confirmPasswordError = AuthValidation.confirmPasswordError(it, confirmPassword)
                }
            },
            isPassword = true,
            errorText = passwordError,
        )
        Spacer(modifier = Modifier.height(OrbitSpacing.lg))
        OrbitTextField(
            label = "CONFIRM PASSWORD",
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                confirmPasswordError = null
            },
            isPassword = true,
            errorText = confirmPasswordError,
        )

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(OrbitSpacing.lg))
            Text(
                text = uiState.errorMessage,
                style = OrbitTypography.bodyMedium,
                color = OrbitColors.danger,
            )
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.xxl))

        if (uiState.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = OrbitColors.violet600)
            }
        } else {
            OrbitGradientButton(
                text = "Create account",
                onClick = {
                    val nErr = AuthValidation.nameError(name)
                    val eErr = AuthValidation.emailError(email)
                    val pErr = AuthValidation.passwordError(password)
                    val cErr = AuthValidation.confirmPasswordError(password, confirmPassword)
                    nameError = nErr
                    emailError = eErr
                    passwordError = pErr
                    confirmPasswordError = cErr
                    if (nErr == null && eErr == null && pErr == null && cErr == null) {
                        onCreateAccountClick(name, email, password)
                    }
                },
            )
        }
    }
}
