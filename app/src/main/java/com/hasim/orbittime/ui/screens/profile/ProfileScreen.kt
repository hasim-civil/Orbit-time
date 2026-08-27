package com.hasim.orbittime.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.ui.components.OrbitFloatingNavContentClearance
import com.hasim.orbittime.ui.components.OrbitFloatingNavHost
import com.hasim.orbittime.ui.components.OrbitTab
import com.hasim.orbittime.ui.components.OrbitTopAppBar
import com.hasim.orbittime.ui.components.cardRiseEntrance
import com.hasim.orbittime.ui.screens.welcome.OrbitAtmosphereBackground
import com.hasim.orbittime.ui.theme.InstrumentSerif
import com.hasim.orbittime.ui.theme.Manrope
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import com.hasim.orbittime.util.ImageCodec
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class ProfileSubScreen { MAIN, EDIT_PROFILE, HOLIDAY_LIST, ADD_LEAVE }

// Exact values measured from the reference `Orbit Time.html` Profile section
// (the `isProfile` sc-if block) — sizes/weights/colors not already covered by the
// shared OrbitTypography scale are defined locally here rather than approximated
// onto an existing style, per the "match exactly" requirement for this screen.
private val MenuRowShape = RoundedCornerShape(22.dp)
private val NameStyle = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 30.sp)
private val SubtitleStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 12.sp)
private val RowLabelStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
private val RowHintStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 11.5.sp)
private val SectionLabelStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.9.sp)
private val DetailKeyStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 12.5.sp)
private val DetailValueStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
private val SignOutStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
private val FooterCreditStyle = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = 17.sp)
private val FooterVersionStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 10.sp, letterSpacing = 1.sp)

private val ShiftFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private fun formatShiftTime(value: String): String? =
    runCatching { ShiftFormatter.format(LocalTime.parse(value)).lowercase(Locale.getDefault()) }.getOrNull()

@Composable
fun ProfileScreen(
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    onSignOutClick: () -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var subScreen by remember { mutableStateOf(ProfileSubScreen.MAIN) }

    BackHandler(enabled = subScreen != ProfileSubScreen.MAIN) {
        subScreen = ProfileSubScreen.MAIN
    }

    when (subScreen) {
        ProfileSubScreen.MAIN -> ProfileContent(
            uiState = uiState,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onSignOutClick = onSignOutClick,
            onEditProfileClick = { subScreen = ProfileSubScreen.EDIT_PROFILE },
            onHolidayListClick = { subScreen = ProfileSubScreen.HOLIDAY_LIST },
            onAddLeaveClick = { subScreen = ProfileSubScreen.ADD_LEAVE },
        )
        ProfileSubScreen.EDIT_PROFILE -> EditProfileScreen(
            onBackClick = { subScreen = ProfileSubScreen.MAIN },
            onSaved = {
                viewModel.refresh()
                subScreen = ProfileSubScreen.MAIN
            },
            onAccountDeleted = onAccountDeleted,
        )
        ProfileSubScreen.HOLIDAY_LIST -> HolidayListScreen(
            onBackClick = { subScreen = ProfileSubScreen.MAIN },
        )
        ProfileSubScreen.ADD_LEAVE -> AddLeaveScreen(
            onBackClick = { subScreen = ProfileSubScreen.MAIN },
        )
    }
}

@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    onSignOutClick: () -> Unit,
    onEditProfileClick: () -> Unit = {},
    onHolidayListClick: () -> Unit = {},
    onAddLeaveClick: () -> Unit = {},
) {
    var showSignOutConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitAtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            OrbitTopAppBar(
                userInitials = uiState.initials,
                onAvatarClick = { onTabSelected(OrbitTab.PROFILE) },
            )

            OrbitFloatingNavHost(selectedTab = selectedTab, onTabSelected = onTabSelected, modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = OrbitSpacing.screenHorizontal)
                        .cardRiseEntrance(),
                    verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
                ) {
                    Spacer(modifier = Modifier.height(OrbitSpacing.xs))

                    ProfileHeaderCard(uiState, onAvatarClick = onEditProfileClick)

                    ProfileMenuRow(
                        icon = { PersonIcon() },
                        iconBackground = OrbitColors.accentBg,
                        label = "Edit profile",
                        hint = "Photo, name, role, shift, email, password",
                        onClick = onEditProfileClick,
                    )
                    ProfileMenuRow(
                        icon = { CalendarIcon() },
                        iconBackground = OrbitColors.infoBg,
                        label = "Holiday list",
                        hint = "Your personal holiday calendar",
                        onClick = onHolidayListClick,
                    )
                    ProfileMenuRow(
                        icon = { PlusIcon() },
                        iconBackground = OrbitColors.successBg,
                        label = "Add leave",
                        hint = "Track your own leave",
                        onClick = onAddLeaveClick,
                    )

                    WorkDetailsCard(uiState)

                    SignOutRow(onClick = { showSignOutConfirm = true })

                    FooterCredits()

                    Spacer(modifier = Modifier.height(OrbitFloatingNavContentClearance))
                }
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text(text = "Sign out?", style = OrbitTypography.titleMedium) },
            text = { Text(text = "You'll need to sign in again to continue.", style = OrbitTypography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutConfirm = false
                    onSignOutClick()
                }) { Text("Sign out", color = OrbitColors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

/**
 * The dark avatar/name header card. Its `radial-gradient(120% 100% at 20% 0%, #3d1f7a,
 * #180e2e, #0a0612)` background is approximated with a linear gradient across the same three
 * stops (#3d1f7a/#180e2e/#0a0612 match OrbitColors.void200/void500/void800 almost exactly) —
 * the same approximation this app's other hero cards (Punch, Monthly Attendance) already make
 * for their own radial-gradient references, so this stays visually consistent with them.
 */
@Composable
private fun ProfileHeaderCard(uiState: ProfileUiState, onAvatarClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(OrbitColors.void200, OrbitColors.void500, OrbitColors.void800),
                ),
            ),
    ) {
        // The reference's small blurred coral blob drifting in the header's top-right corner —
        // reproduced as a soft gradient-falloff glow (see OrbitAtmosphereBackground for why this
        // app never relies on Modifier.blur) with a gentle breathing alpha instead of full drift,
        // since it's a small decorative accent rather than the primary background effect.
        val infinite = rememberInfiniteTransition(label = "profileHeaderGlow")
        val breathe = infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "breathe",
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.TopEnd)
                .graphicsLayer {
                    translationX = 40.dp.toPx()
                    translationY = (-60).dp.toPx()
                    alpha = 0.55f + breathe.value * 0.45f
                }
                .background(
                    brush = Brush.radialGradient(
                        0f to OrbitColors.coral500.copy(alpha = 0.22f),
                        0.72f to OrbitColors.coral500.copy(alpha = 0.06f),
                        1f to Color.Transparent,
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).clickable(onClick = onAvatarClick)) {
                val decodedPhoto = remember(uiState.photoBase64) {
                    uiState.photoBase64.takeIf { it.isNotBlank() }?.let { ImageCodec.decodeToImageBitmap(it) }
                }
                if (decodedPhoto != null) {
                    Image(
                        bitmap = decodedPhoto,
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(OrbitColors.purple500, OrbitColors.blue500),
                                ),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uiState.initials,
                            style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 20.sp),
                            color = Color.White,
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.name,
                    style = NameStyle,
                    color = OrbitColors.lavenderWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = uiState.email,
                    style = SubtitleStyle,
                    color = OrbitColors.lavenderWhite.copy(alpha = 0.64f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: @Composable () -> Unit,
    iconBackground: Color,
    label: String,
    hint: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.985f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(MenuRowShape)
            .background(OrbitColors.cream50.copy(alpha = 0.96f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(iconBackground, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = RowLabelStyle, color = OrbitColors.ink900)
            Text(text = hint, style = RowHintStyle, color = OrbitColors.slate600, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(text = "›", style = RowLabelStyle, color = OrbitColors.slate300)
    }
}

@Composable
private fun WorkDetailsCard(uiState: ProfileUiState) {
    val shiftLabel = if (uiState.shiftStart != null && uiState.shiftEnd != null) {
        val start = formatShiftTime(uiState.shiftStart)
        val end = formatShiftTime(uiState.shiftEnd)
        if (start != null && end != null) "$start – $end" else null
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(OrbitColors.cream50.copy(alpha = 0.96f))
            .padding(20.dp),
    ) {
        Text(text = "WORK DETAILS", style = SectionLabelStyle, color = OrbitColors.slate500)
        Spacer(modifier = Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            DetailRow(key = "Email", value = uiState.email.ifBlank { "—" })
            if (uiState.role.isNotBlank()) {
                DetailRow(key = "Role", value = uiState.role)
            }
            if (shiftLabel != null) {
                DetailRow(key = "Shift", value = shiftLabel)
            }
        }
    }
}

@Composable
private fun DetailRow(key: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = key, style = DetailKeyStyle, color = OrbitColors.slate600, maxLines = 1)
        Text(text = value, style = DetailValueStyle, color = OrbitColors.ink900, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SignOutRow(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MenuRowShape)
            .background(OrbitColors.cream50.copy(alpha = 0.96f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(17.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Sign out", style = SignOutStyle, color = OrbitColors.danger)
    }
}

@Composable
private fun FooterCredits() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Designed & Developed by Hasim",
            style = FooterCreditStyle,
            color = OrbitColors.ink900.copy(alpha = 0.42f),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "ORBIT TIME v1.0",
            style = FooterVersionStyle,
            color = OrbitColors.slate500.copy(alpha = 0.75f),
        )
    }
}

// --- Icons, drawn on the reference's exact 20x20 viewBox path data. ---

@Composable
private fun PersonIcon(size: Dp = 16.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val scale = this.size.minDimension / 20f
        fun px(v: Float) = v * scale
        drawCircle(color = OrbitColors.violet600, radius = px(3.1f), center = Offset(px(10f), px(7f)), style = Stroke(width = px(1.6f)))
        val path = Path().apply {
            moveTo(px(4f), px(16.5f))
            cubicTo(px(4.6f), px(13.5f), px(6.9f), px(12f), px(10f), px(12f))
            cubicTo(px(13.1f), px(12f), px(15.4f), px(13.5f), px(16f), px(16.5f))
        }
        drawPath(path, color = OrbitColors.violet600, style = Stroke(width = px(1.6f)))
    }
}

@Composable
private fun CalendarIcon(size: Dp = 16.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val scale = this.size.minDimension / 20f
        fun px(v: Float) = v * scale
        drawRoundRect(
            color = OrbitColors.blue500,
            topLeft = Offset(px(3f), px(4.5f)),
            size = androidx.compose.ui.geometry.Size(px(14f), px(12f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(px(3f), px(3f)),
            style = Stroke(width = px(1.6f)),
        )
        drawLine(OrbitColors.blue500, Offset(px(3f), px(8.5f)), Offset(px(17f), px(8.5f)), strokeWidth = px(1.6f))
        drawLine(OrbitColors.blue500, Offset(px(7f), px(3f)), Offset(px(7f), px(6f)), strokeWidth = px(1.6f))
        drawLine(OrbitColors.blue500, Offset(px(13f), px(3f)), Offset(px(13f), px(6f)), strokeWidth = px(1.6f))
    }
}

@Composable
private fun PlusIcon(size: Dp = 16.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val scale = this.size.minDimension / 20f
        fun px(v: Float) = v * scale
        drawLine(OrbitColors.successDark, Offset(px(10f), px(4.5f)), Offset(px(10f), px(15.5f)), strokeWidth = px(1.6f))
        drawLine(OrbitColors.successDark, Offset(px(4.5f), px(10f)), Offset(px(15.5f), px(10f)), strokeWidth = px(1.6f))
    }
}
