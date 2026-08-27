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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val ShiftClockFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private fun LocalTime.toShiftLabel(): String = ShiftClockFormatter.format(this).lowercase(Locale.getDefault())

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
        onCreateAccountClick = { name, email, password, shiftStart, shiftEnd ->
            viewModel.createAccount(name, email, password, shiftStart, shiftEnd, onAccountCreated)
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

@Composable
fun CreateAccountContent(
    uiState: AuthUiState,
    onBackClick: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onCreateAccountClick: (name: String, email: String, password: String, shiftStart: LocalTime, shiftEnd: LocalTime) -> Unit,
    onGoogleSignInClick: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var shiftStart by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var shiftEnd by remember { mutableStateOf(LocalTime.of(17, 30)) }

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
        ShiftSelectorField(
            shiftStart = shiftStart,
            shiftEnd = shiftEnd,
            onShiftChanged = { start, end -> shiftStart = start; shiftEnd = end },
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
                    nameError = nErr
                    emailError = eErr
                    passwordError = pErr
                    if (nErr == null && eErr == null && pErr == null) {
                        onCreateAccountClick(name, email, password, shiftStart, shiftEnd)
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

/**
 * Reference "SHIFT" row: same cream-box label field as [OrbitTextField]. Tapping it opens a
 * dialog to set the user's own shift start/end time — there's no fixed set of shifts, and each
 * user's late-arrival threshold is later computed from exactly this start time (see
 * [com.hasim.orbittime.util.AttendanceStats]), so it has to be a real time, not a preset label.
 */
@Composable
private fun ShiftSelectorField(
    shiftStart: LocalTime,
    shiftEnd: LocalTime,
    onShiftChanged: (LocalTime, LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

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
                .clickable { showDialog = true }
                .padding(horizontal = OrbitSpacing.lg, vertical = OrbitSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${shiftStart.toShiftLabel()} – ${shiftEnd.toShiftLabel()}",
                style = OrbitTypography.bodyLarge,
                color = OrbitColors.ink900,
            )
            Text(
                text = "⌄",
                style = OrbitTypography.titleMedium,
                color = OrbitColors.slate500,
            )
        }
    }

    if (showDialog) {
        ShiftTimeDialog(
            initialStart = shiftStart,
            initialEnd = shiftEnd,
            onConfirm = { start, end ->
                onShiftChanged(start, end)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftTimeDialog(
    initialStart: LocalTime,
    initialEnd: LocalTime,
    onConfirm: (LocalTime, LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val startState = rememberTimePickerState(initialHour = initialStart.hour, initialMinute = initialStart.minute, is24Hour = false)
    val endState = rememberTimePickerState(initialHour = initialEnd.hour, initialMinute = initialEnd.minute, is24Hour = false)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Set your shift", style = OrbitTypography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                Text(text = "Shift start", style = OrbitTypography.label, color = OrbitColors.slate500)
                TimeInput(state = startState)
                Spacer(modifier = Modifier.height(OrbitSpacing.xs))
                Text(text = "Shift end", style = OrbitTypography.label, color = OrbitColors.slate500)
                TimeInput(state = endState)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    LocalTime.of(startState.hour, startState.minute),
                    LocalTime.of(endState.hour, endState.minute),
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
