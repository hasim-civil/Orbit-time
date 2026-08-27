package com.hasim.orbittime.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hasim.orbittime.ui.components.AuthScreenScaffold
import com.hasim.orbittime.ui.components.InlineBanner
import com.hasim.orbittime.ui.components.OrbitGradientButton
import com.hasim.orbittime.ui.components.OrbitTextField
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import com.hasim.orbittime.util.ImageCodec
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val EditProfileShiftFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private fun LocalTime.toDisplayLabel(): String = EditProfileShiftFormatter.format(this).lowercase(Locale.getDefault())

@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: EditProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.dismissError() }

    var name by remember(uiState.isLoading) { mutableStateOf(uiState.name) }
    var email by remember(uiState.isLoading) { mutableStateOf(uiState.email) }
    var role by remember(uiState.isLoading) { mutableStateOf(uiState.role) }
    var password by remember { mutableStateOf("") }
    var shiftStart by remember(uiState.isLoading) { mutableStateOf(uiState.shiftStart) }
    var shiftEnd by remember(uiState.isLoading) { mutableStateOf(uiState.shiftEnd) }
    var showShiftDialog by remember { mutableStateOf(false) }
    var showDeleteWarning by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri)
    }

    AuthScreenScaffold(
        headline = "Edit profile",
        subtitle = "Update your photo, name, role, shift, email or password.",
        onBackClick = onBackClick,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .clickable { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center,
            ) {
                val localPreview = uiState.localPhotoPreview
                val decodedPhoto = remember(uiState.photoBase64) {
                    uiState.photoBase64.takeIf { it.isNotBlank() }?.let { ImageCodec.decodeToImageBitmap(it) }
                }
                when {
                    localPreview != null -> AsyncImage(
                        model = localPreview,
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(88.dp).clip(CircleShape),
                    )
                    decodedPhoto != null -> Image(
                        bitmap = decodedPhoto,
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(88.dp).clip(CircleShape),
                    )
                    else -> Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(OrbitColors.purple500, OrbitColors.blue500),
                                ),
                                shape = CircleShape,
                            ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(OrbitSpacing.xs))
            Text(text = "Tap to change photo", style = OrbitTypography.bodySmall, color = OrbitColors.slate600)
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.xl))

        OrbitTextField(label = "FULL NAME", value = name, onValueChange = { name = it })
        Spacer(modifier = Modifier.height(OrbitSpacing.lg))
        OrbitTextField(label = "EMAIL", value = email, onValueChange = { email = it }, keyboardType = KeyboardType.Email)
        Spacer(modifier = Modifier.height(OrbitSpacing.lg))
        OrbitTextField(label = "ROLE", value = role, onValueChange = { role = it })
        Spacer(modifier = Modifier.height(OrbitSpacing.lg))

        Column {
            Text(text = "SHIFT", style = OrbitTypography.label, color = OrbitColors.slate500)
            Spacer(modifier = Modifier.height(OrbitSpacing.xs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OrbitColors.cream50, OrbitShapes.medium)
                    .clickable { showShiftDialog = true }
                    .padding(horizontal = OrbitSpacing.lg, vertical = OrbitSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "${shiftStart.toDisplayLabel()} – ${shiftEnd.toDisplayLabel()}", style = OrbitTypography.bodyLarge, color = OrbitColors.ink900)
                Text(text = "⌄", style = OrbitTypography.titleMedium, color = OrbitColors.slate500)
            }
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.lg))
        OrbitTextField(
            label = "NEW PASSWORD (OPTIONAL)",
            value = password,
            onValueChange = { password = it },
            isPassword = true,
        )

        val errorMessage = uiState.errorMessage
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(OrbitSpacing.lg))
            InlineBanner(text = errorMessage, color = OrbitColors.danger, background = OrbitColors.dangerBg)
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.xxl))

        if (uiState.isSaving) {
            Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrbitColors.violet600)
            }
        } else {
            OrbitGradientButton(
                text = "Save changes",
                onClick = { viewModel.save(name, email, role, shiftStart, shiftEnd, password, onSaved) },
            )
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.xxl))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrbitColors.dangerBg, OrbitShapes.medium)
                .clickable { showDeleteWarning = true }
                .padding(vertical = OrbitSpacing.md),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = "Delete account", style = OrbitTypography.titleMedium, color = OrbitColors.danger)
        }
    }

    if (showDeleteWarning) {
        AlertDialog(
            onDismissRequest = { showDeleteWarning = false },
            title = { Text(text = "Delete account?", style = OrbitTypography.titleMedium) },
            text = {
                Text(
                    text = "Are you sure? This will permanently delete your account and all your data.",
                    style = OrbitTypography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteWarning = false
                    showDeleteConfirm = true
                }) { Text("Continue", color = OrbitColors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteWarning = false }) { Text("Cancel") }
            },
        )
    }

    val awaitingReauth = uiState.needsReauthToDelete
    if (showDeleteConfirm || awaitingReauth) {
        DeleteAccountDialog(
            isDeleting = uiState.isDeletingAccount,
            errorMessage = uiState.deleteError,
            awaitingReauth = awaitingReauth,
            canReauthWithPassword = uiState.canReauthWithPassword,
            onConfirmDelete = { viewModel.deleteAccount(password = null, onDeleted = onAccountDeleted) },
            onConfirmReauth = { reauthPassword -> viewModel.deleteAccount(password = reauthPassword, onDeleted = onAccountDeleted) },
            onDismiss = {
                showDeleteConfirm = false
                viewModel.dismissReauth()
            },
        )
    }

    if (showShiftDialog) {
        EditShiftDialog(
            initialStart = shiftStart,
            initialEnd = shiftEnd,
            onConfirm = { start, end ->
                shiftStart = start
                shiftEnd = end
                showShiftDialog = false
            },
            onDismiss = { showShiftDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditShiftDialog(
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

/**
 * The final, irreversible confirmation — reused for the re-authentication retry too, since
 * Firebase may require a fresher sign-in before it will actually delete the Auth account.
 * While [isDeleting] the dialog can't be dismissed, so a slow delete can't be double-tapped
 * or backed out of mid-flight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteAccountDialog(
    isDeleting: Boolean,
    errorMessage: String?,
    awaitingReauth: Boolean,
    canReauthWithPassword: Boolean,
    onConfirmDelete: () -> Unit,
    onConfirmReauth: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reauthPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = {
            Text(
                text = if (awaitingReauth) "Confirm your password" else "This can't be undone",
                style = OrbitTypography.titleMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                when {
                    awaitingReauth && canReauthWithPassword -> {
                        Text(
                            text = "For your security, please re-enter your password to finish deleting your account.",
                            style = OrbitTypography.bodyMedium,
                        )
                        OrbitTextField(
                            label = "PASSWORD",
                            value = reauthPassword,
                            onValueChange = { reauthPassword = it },
                            isPassword = true,
                        )
                    }
                    awaitingReauth -> {
                        Text(
                            text = "For your security, please sign out and sign in again, then retry deleting your account.",
                            style = OrbitTypography.bodyMedium,
                        )
                    }
                    else -> {
                        Text(
                            text = "Confirm again to permanently delete your account and all your data — attendance, holidays, leave records and your profile. This cannot be reversed.",
                            style = OrbitTypography.bodyMedium,
                        )
                    }
                }
                if (errorMessage != null) {
                    InlineBanner(text = errorMessage, color = OrbitColors.danger, background = OrbitColors.dangerBg)
                }
            }
        },
        confirmButton = {
            if (isDeleting) {
                Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrbitColors.danger, modifier = Modifier.size(20.dp))
                }
            } else if (awaitingReauth) {
                if (canReauthWithPassword) {
                    TextButton(
                        onClick = { onConfirmReauth(reauthPassword) },
                        enabled = reauthPassword.isNotBlank(),
                    ) { Text("Confirm & delete", color = OrbitColors.danger) }
                }
            } else {
                TextButton(onClick = onConfirmDelete) { Text("Delete permanently", color = OrbitColors.danger) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) { Text("Cancel") }
        },
    )
}
