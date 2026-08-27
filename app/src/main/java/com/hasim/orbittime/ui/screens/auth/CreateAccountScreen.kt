package com.hasim.orbittime.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.R
import com.hasim.orbittime.data.auth.AuthValidation
import com.hasim.orbittime.ui.components.AuthScreenScaffold
import com.hasim.orbittime.ui.components.OrDivider
import com.hasim.orbittime.ui.components.OrbitGradientButton
import com.hasim.orbittime.ui.components.OrbitOutlineButton
import com.hasim.orbittime.ui.components.OrbitTextField
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import kotlinx.coroutines.launch

@Composable
fun CreateAccountScreen(
    onBackClick: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onAccountCreated: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    val scope = rememberCoroutineScope()

    CreateAccountContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onNavigateToSignIn = onNavigateToSignIn,
        onCreateAccountClick = { name, email, password, shift ->
            viewModel.createAccount(name, email, password, shift, onAccountCreated)
        },
        onGoogleSignInClick = {
            scope.launch {
                requestGoogleIdToken(context, webClientId)
                    .onSuccess { idToken -> viewModel.signInWithGoogle(idToken, onAccountCreated) }
                    .onFailure { error ->
                        if (error !is GetCredentialCancellationException) {
                            viewModel.reportError("Google sign-in failed. Please try again.")
                        }
                    }
            }
        },
    )
}

/** Reference shift options — no shift-schedule backend exists yet, so this is a fixed list. */
val ShiftOptions = listOf(
    "Morning · 9:00 am – 5:30 pm",
    "Afternoon · 1:00 pm – 9:30 pm",
    "Night · 9:00 pm – 5:30 am",
)

@Composable
fun CreateAccountContent(
    uiState: AuthUiState,
    onBackClick: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onCreateAccountClick: (name: String, email: String, password: String, shift: String) -> Unit,
    onGoogleSignInClick: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var shift by remember { mutableStateOf(ShiftOptions.first()) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    AuthScreenScaffold(
        headline = "Create account",
        subtitle = "Set up your profile and start logging hours today.",
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
            },
            isPassword = true,
            errorText = passwordError,
        )
        Spacer(modifier = Modifier.height(OrbitSpacing.lg))
        ShiftSelectorField(selectedShift = shift, onShiftSelected = { shift = it })

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
                    nameError = nErr
                    emailError = eErr
                    passwordError = pErr
                    if (nErr == null && eErr == null && pErr == null) {
                        onCreateAccountClick(name, email, password, shift)
                    }
                },
            )
            Spacer(modifier = Modifier.height(OrbitSpacing.sm))
            Text(
                text = "By continuing you agree to Orbit Time's terms and privacy policy.",
                style = OrbitTypography.bodySmall,
                color = OrbitColors.slate500,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(OrbitSpacing.lg))
            OrDivider()
            Spacer(modifier = Modifier.height(OrbitSpacing.lg))
            OrbitOutlineButton(text = "Continue with Google", onClick = onGoogleSignInClick)
        }
    }
}

/** Reference "SHIFT" row: same cream-box label field as [OrbitTextField], styled as a dropdown. */
@Composable
private fun ShiftSelectorField(
    selectedShift: String,
    onShiftSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "SHIFT",
            style = OrbitTypography.label,
            color = OrbitColors.slate500,
        )
        Spacer(modifier = Modifier.height(OrbitSpacing.xs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrbitColors.cream50, OrbitShapes.medium)
                .clickable { expanded = true }
                .padding(horizontal = OrbitSpacing.lg, vertical = OrbitSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedShift,
                style = OrbitTypography.bodyLarge,
                color = OrbitColors.ink900,
            )
            Text(
                text = "⌄",
                style = OrbitTypography.titleMedium,
                color = OrbitColors.slate500,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ShiftOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onShiftSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
