package com.hasim.orbittime.ui.screens.notifications

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.data.notification.NotificationKind
import com.hasim.orbittime.data.notification.UserNotification
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
import java.time.Duration
import java.time.Instant

private val TitleStyle = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = 27.sp)
private val ClearAllStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
private val CardTitleStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
private val CardBodyStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
private val CardTimeStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 11.sp)
private val EmptyHeadlineStyle = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = 23.sp)
private val EmptyBodyStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 12.5.sp)

/** A live Firestore-backed feed of the user's own attendance alerts, matching the reference's
 * "isAlerts" page — reached by tapping the bell in [OrbitTopAppBar] from any tab. */
@Composable
fun NotificationsScreen(
    userInitials: String,
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    photoBase64: String = "",
    viewModel: NotificationsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitAtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            OrbitTopAppBar(
                userInitials = userInitials,
                photoBase64 = photoBase64,
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Notifications", style = TitleStyle, color = OrbitColors.ink900)
                        if (uiState.notifications.isNotEmpty()) {
                            Text(
                                text = "Clear all",
                                style = ClearAllStyle,
                                color = OrbitColors.violet600,
                                modifier = Modifier.clickable(onClick = viewModel::clearAll),
                            )
                        }
                    }

                    if (uiState.notifications.isEmpty()) {
                        EmptyNotificationsCard()
                    } else {
                        uiState.notifications.forEach { notification ->
                            NotificationCard(notification = notification, onClick = { viewModel.markRead(notification.id) })
                        }
                    }

                    Spacer(modifier = Modifier.height(OrbitFloatingNavContentClearance))
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: UserNotification, onClick: () -> Unit) {
    val (tint, ink) = notification.kind.colors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(OrbitColors.cream50.copy(alpha = if (notification.read) 0.7f else 0.96f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(tint, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(3.dp)).background(ink))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.title,
                    style = CardTitleStyle,
                    color = OrbitColors.ink900,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!notification.read) {
                    Spacer(modifier = Modifier.width(OrbitSpacing.xs))
                    Box(modifier = Modifier.size(6.dp).background(OrbitColors.violet600, CircleShape))
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = notification.body, style = CardBodyStyle, color = OrbitColors.slate600)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = relativeTimeLabel(notification.createdAt?.toDate()?.toInstant()), style = CardTimeStyle, color = OrbitColors.slate300)
        }
    }
}

@Composable
private fun EmptyNotificationsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(OrbitColors.cream50.copy(alpha = 0.96f))
            .padding(vertical = 46.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(66.dp).background(OrbitColors.mist, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(26.dp)) {
                drawOval(
                    color = OrbitColors.slate200,
                    topLeft = Offset(size.width * 0.06f, size.height * 0.24f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.88f, size.height * 0.52f),
                    style = Stroke(width = size.minDimension * 0.08f),
                )
                drawCircle(color = OrbitColors.slate200, radius = size.minDimension * 0.14f, center = center)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "You're all caught up", style = EmptyHeadlineStyle, color = OrbitColors.ink900, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "No attendance alerts right now. We'll let you know if something needs your attention.",
            style = EmptyBodyStyle,
            color = OrbitColors.slate600,
            textAlign = TextAlign.Center,
        )
    }
}

private fun NotificationKind.colors(): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> = when (this) {
    NotificationKind.LATE_ARRIVAL -> OrbitColors.dangerBg to OrbitColors.danger
    NotificationKind.ATTENDANCE_INFO -> OrbitColors.accentBg to OrbitColors.accent
    NotificationKind.SHIFT_REMINDER -> OrbitColors.successBg to OrbitColors.success
}

private fun relativeTimeLabel(instant: Instant?): String {
    if (instant == null) return ""
    val minutes = Duration.between(instant, Instant.now()).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        minutes < 60 * 24 * 2 -> "Yesterday"
        else -> "${minutes / (60 * 24)} days ago"
    }
}
