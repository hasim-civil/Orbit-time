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
fun SignInScreen(
    onBackClick: () -> Unit,
    onNavigateToCreateAccount: () -> Unit,
    onSignedIn: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    SignInContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onNavigateToCreateAccount = onNavigateToCreateAccount,
        onSignInClick = { email, password -> viewModel.signIn(email, password, onSignedIn) },
    )
}

@Composable
fun SignInContent(
    uiState: AuthUiState,
    onBackClick: () -> Unit,
    onNavigateToCreateAccount: () -> Unit,
    onSignInClick: (email: String, password: String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailTouchedError by remember { mutableStateOf<String?>(null) }
    var passwordTouchedError by remember { mutableStateOf<String?>(null) }

    AuthScreenScaffold(
        headline = "Welcome back",
        subtitle = "Sign in to pick up where your orbit left off.",
        onBackClick = onBackClick,
        footer = {
            Row {
                Text(
                    text = "New to Orbit Time? ",
                    style = OrbitTypography.bodyMedium,
                    color = OrbitColors.slate600,
                )
                Text(
                    text = "Create account",
                    style = OrbitTypography.titleMedium,
                    color = OrbitColors.violet600,
                    modifier = Modifier.clickable(onClick = onNavigateToCreateAccount),
                )
            }
        },
    ) {
        OrbitTextField(
            label = "WORK EMAIL",
            value = email,
            onValueChange = {
                email = it
                emailTouchedError = null
            },
            keyboardType = KeyboardType.Email,
            errorText = emailTouchedError,
        )
        Spacer(modifier = Modifier.height(OrbitSpacing.lg))
        OrbitTextField(
            label = "PASSWORD",
            value = password,
            onValueChange = {
                password = it
                passwordTouchedError = null
            },
            isPassword = true,
            errorText = passwordTouchedError,
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
                text = "Sign in",
                onClick = {
                    val emailError = AuthValidation.emailError(email)
                    val passwordError = AuthValidation.passwordError(password)
                    emailTouchedError = emailError
                    passwordTouchedError = passwordError
                    if (emailError == null && passwordError == null) {
                        onSignInClick(email, password)
                    }
                },
            )
        }
    }
}
