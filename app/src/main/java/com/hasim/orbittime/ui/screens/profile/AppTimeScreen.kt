package com.hasim.orbittime.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasim.orbittime.data.settings.AppTimeSettingsStore
import com.hasim.orbittime.ui.components.AuthScreenScaffold
import com.hasim.orbittime.ui.components.OrbitGradientButton
import com.hasim.orbittime.ui.components.OrbitOutlineButton
import com.hasim.orbittime.ui.components.rememberAppTimeNow
import com.hasim.orbittime.ui.theme.InstrumentSerif
import com.hasim.orbittime.ui.theme.Manrope
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import com.hasim.orbittime.util.AppTime
import com.hasim.orbittime.util.AttendanceTimeFormat
import com.hasim.orbittime.util.OrbitClock
import java.time.LocalTime
import kotlin.math.abs
import kotlinx.coroutines.launch

private val WheelItemHeight = 42.dp
private val WheelGapWidth = 6.dp
private val AmPmWidth = 56.dp
private const val WHEEL_VISIBLE_ITEMS = 5

private val LiveClockStyle = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 42.sp)
private val WheelValueStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
private val ColonStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 20.sp)

private val HOUR_ITEMS = (1..12).map { it.toString() }
private val MINUTE_ITEMS = (0..59).map { "%02d".format(it) }
private val SECOND_ITEMS = (0..59).map { "%02d".format(it) }

/**
 * Profile → App Time: whether Orbit Time reads the device clock or a time the user sets here.
 *
 * The screen only ever writes [AppTimeSettingsStore]; every calculation in the app reads that
 * same setting back through [OrbitClock], so there is one clock, not one per screen. Nothing
 * here touches the device's own clock — the override is internal to Orbit Time.
 */
@Composable
fun AppTimeScreen(onBackClick: () -> Unit) {
    val settings by AppTimeSettingsStore.settingsFlow.collectAsState()
    val now by rememberAppTimeNow()

    // The picker's own draft. Seeded from whatever the app clock currently reads, so opening the
    // screen never starts from an arbitrary time — and re-seeded whenever the saved mode changes
    // (e.g. after switching back to device time), so the wheels agree with what is in force.
    var draft by remember(settings.mode) {
        mutableStateOf(settings.customTime ?: OrbitClock.localTime())
    }
    var editingCustom by remember(settings.mode) { mutableStateOf(settings.isManual) }

    AuthScreenScaffold(
        headline = "App time",
        subtitle = "Choose the clock Orbit Time runs on. Your phone's own clock is never changed.",
        onBackClick = onBackClick,
        footer = {
            if (editingCustom) {
                OrbitGradientButton(
                    text = if (settings.isManual) "Update app time" else "Use this time",
                    onClick = { AppTimeSettingsStore.setManualTime(draft) },
                )
                if (settings.isManual) {
                    Spacer(modifier = Modifier.height(OrbitSpacing.sm))
                    OrbitOutlineButton(
                        text = "Switch back to device time",
                        onClick = { AppTimeSettingsStore.useDeviceTime() },
                    )
                }
            }
        },
    ) {
        CurrentTimeCard(
            clockLabel = AttendanceTimeFormat.clockTimeWithSeconds(now),
            isManual = settings.isManual,
            customTime = settings.customTime,
        )

        Spacer(modifier = Modifier.height(OrbitSpacing.lg))

        SourceToggle(
            isManualSelected = editingCustom,
            // Device time applies the moment it's chosen — switching back should never need a
            // second confirming tap.
            onDeviceSelected = {
                editingCustom = false
                AppTimeSettingsStore.useDeviceTime()
            },
            // Custom only opens the picker; nothing changes until the user confirms below.
            onCustomSelected = {
                if (!editingCustom) draft = OrbitClock.localTime()
                editingCustom = true
            },
        )

        if (editingCustom) {
            Spacer(modifier = Modifier.height(OrbitSpacing.lg))
            TimeWheelPicker(value = draft, onValueChange = { draft = it })
            Spacer(modifier = Modifier.height(OrbitSpacing.sm))
            Text(
                text = "Orbit Time keeps counting from the time you set — check-in, check-out, " +
                    "worked hours and overtime all follow this clock.",
                style = OrbitTypography.bodySmall,
                color = OrbitColors.slate600,
            )
        }
    }
}

/** The dark hero card: what time the app is on right now, and where that time comes from. */
@Composable
private fun CurrentTimeCard(clockLabel: String, isManual: Boolean, customTime: LocalTime?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OrbitShapes.card)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(OrbitColors.void200, OrbitColors.void500, OrbitColors.void800),
                ),
            )
            .padding(horizontal = OrbitSpacing.xl, vertical = OrbitSpacing.lg),
    ) {
        Text(text = "ORBIT TIME NOW", style = OrbitTypography.label, color = OrbitColors.slate300)
        Spacer(modifier = Modifier.height(OrbitSpacing.xs))
        Text(text = clockLabel, style = LiveClockStyle, color = OrbitColors.lavenderWhite)
        Spacer(modifier = Modifier.height(OrbitSpacing.sm))

        val sourceColor = if (isManual) OrbitColors.coral400 else OrbitColors.success
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(sourceColor.copy(alpha = 0.16f), CircleShape)
                .padding(horizontal = 11.dp, vertical = 6.dp),
        ) {
            Box(modifier = Modifier.size(7.dp).background(sourceColor, CircleShape))
            Spacer(modifier = Modifier.width(OrbitSpacing.xs))
            Text(
                text = if (isManual) "Custom time" else "Device time",
                style = OrbitTypography.bodySmall,
                color = sourceColor,
            )
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.xs))
        Text(
            text = if (isManual) {
                val setLabel = customTime?.let { AttendanceTimeFormat.clockTimeWithSeconds(it) }
                if (setLabel == null) "Running on a time you set." else "Set to $setLabel, and counting on from there."
            } else {
                "Following your phone's clock."
            },
            style = OrbitTypography.bodySmall,
            color = OrbitColors.lavenderWhite.copy(alpha = 0.66f),
        )
    }
}

/** Two-segment source picker, the same pill toggle Home uses for its Week/Month range. */
@Composable
private fun SourceToggle(
    isManualSelected: Boolean,
    onDeviceSelected: () -> Unit,
    onCustomSelected: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.mist, OrbitShapes.pill)
            .border(1.dp, OrbitColors.slate200.copy(alpha = 0.5f), OrbitShapes.pill)
            .padding(4.dp),
    ) {
        ToggleSegment(
            label = "Device time",
            selected = !isManualSelected,
            modifier = Modifier.weight(1f),
            onClick = onDeviceSelected,
        )
        ToggleSegment(
            label = "Custom time",
            selected = isManualSelected,
            modifier = Modifier.weight(1f),
            onClick = onCustomSelected,
        )
    }
}

@Composable
private fun ToggleSegment(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(OrbitShapes.pill)
            .background(if (selected) OrbitColors.violet600 else Color.Transparent, OrbitShapes.pill)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = OrbitTypography.bodySmall,
            color = if (selected) OrbitColors.cream50 else OrbitColors.slate600,
            maxLines = 1,
        )
    }
}

/**
 * Hour / minute / second wheels plus an AM/PM selector, on a cream card with the selected row
 * highlighted — a scroll-and-snap dial rather than typed digits, so a time can be set with a
 * thumb and never needs validating for nonsense input.
 *
 * The 12-hour dial is the only thing shown; [AppTime.localTimeOf] does the am/pm conversion, so
 * 12:00 am (hour 0) and 12:00 pm (hour 12) are handled in one tested place instead of here.
 */
@Composable
private fun TimeWheelPicker(value: LocalTime, onValueChange: (LocalTime) -> Unit) {
    val hour12 = AppTime.hour12Of(value)
    val isAm = AppTime.isAm(value)

    fun emit(newHour12: Int = hour12, newMinute: Int = value.minute, newSecond: Int = value.second, newIsAm: Boolean = isAm) {
        val next = AppTime.localTimeOf(newHour12, newMinute, newSecond, newIsAm)
        if (next != value) onValueChange(next)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.cream50, OrbitShapes.card)
            .padding(horizontal = OrbitSpacing.md, vertical = OrbitSpacing.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            WheelLabel(text = "HOUR", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(WheelGapWidth))
            WheelLabel(text = "MIN", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(WheelGapWidth))
            WheelLabel(text = "SEC", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(OrbitSpacing.sm))
            WheelLabel(text = "AM/PM", modifier = Modifier.width(AmPmWidth))
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.xs))

        // The AM/PM column is a sibling of the scrolling group, not a member of it: the selected-row
        // highlight lives inside that group and fills it, so it can only ever be as wide as the
        // hour/minute/second columns it belongs to. It used to be a full-width child of one Box
        // wrapping all four columns, which is why the band ran on under the AM/PM capsule.
        //
        // The group takes the leftover width by weight rather than a measured or fixed size, so
        // the band ends in the same place relative to AM/PM at any screen width — and each wheel
        // is still an equal weighted share of that group, so the centred values don't move.
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(WheelItemHeight * WHEEL_VISIBLE_ITEMS),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(WheelItemHeight)
                        .background(OrbitColors.accentBg, OrbitShapes.medium),
                )

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TimeWheel(
                        items = HOUR_ITEMS,
                        selectedIndex = hour12 - 1,
                        onSelectedIndexChange = { emit(newHour12 = it + 1) },
                        modifier = Modifier.weight(1f),
                    )
                    WheelColon()
                    TimeWheel(
                        items = MINUTE_ITEMS,
                        selectedIndex = value.minute,
                        onSelectedIndexChange = { emit(newMinute = it) },
                        modifier = Modifier.weight(1f),
                    )
                    WheelColon()
                    TimeWheel(
                        items = SECOND_ITEMS,
                        selectedIndex = value.second,
                        onSelectedIndexChange = { emit(newSecond = it) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.width(OrbitSpacing.sm))
            AmPmSelector(
                isAm = isAm,
                onSelect = { emit(newIsAm = it) },
                modifier = Modifier.width(AmPmWidth),
            )
        }
    }
}

@Composable
private fun WheelLabel(text: String, modifier: Modifier) {
    Text(
        text = text,
        style = OrbitTypography.label,
        color = OrbitColors.slate500,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
private fun WheelColon() {
    Box(
        modifier = Modifier.width(WheelGapWidth).height(WheelItemHeight * WHEEL_VISIBLE_ITEMS),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = ":", style = ColonStyle, color = OrbitColors.slate400)
    }
}

/**
 * One scrolling column. The centred item is the value: [WHEEL_VISIBLE_ITEMS] rows are visible
 * and the list is padded by two rows top and bottom, so index `firstVisibleItemIndex` sits
 * exactly in the middle band — including for the very first and very last item.
 *
 * Snapping is done by animating to the nearest row once the user stops scrolling, rather than
 * with a fling-behaviour API, so the wheel always settles on a whole value and never leaves two
 * half-rows showing.
 */
@Composable
private fun TimeWheel(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex))
    val scope = rememberCoroutineScope()
    val itemHeightPx = with(LocalDensity.current) { WheelItemHeight.toPx() }

    val centredIndex by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val rolledOver = listState.firstVisibleItemScrollOffset > itemHeightPx / 2f
            (if (rolledOver) first + 1 else first).coerceIn(0, items.lastIndex)
        }
    }

    // Settle on a whole row, then report it. Reporting only once the scroll has stopped keeps a
    // long flick from emitting every value it passes through.
    //
    // Watched through snapshotFlow rather than as a LaunchedEffect key on purpose: keying on
    // isScrollInProgress would cancel this very effect the moment animateScrollToItem below
    // started its own scroll, abandoning the wheel mid-row.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { isScrolling ->
            if (!isScrolling) {
                val target = centredIndex
                if (listState.firstVisibleItemScrollOffset != 0) listState.animateScrollToItem(target)
                onSelectedIndexChange(target)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.height(WheelItemHeight * WHEEL_VISIBLE_ITEMS),
        contentPadding = PaddingValues(vertical = WheelItemHeight * ((WHEEL_VISIBLE_ITEMS - 1) / 2)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(items) { index, text ->
            val distance = abs(index - centredIndex)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WheelItemHeight)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        scope.launch { listState.animateScrollToItem(index) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    style = WheelValueStyle,
                    color = if (distance == 0) OrbitColors.ink900 else OrbitColors.slate500,
                    modifier = Modifier.graphicsLayer {
                        alpha = when (distance) {
                            0 -> 1f
                            1 -> 0.5f
                            else -> 0.22f
                        }
                        val scale = if (distance == 0) 1f else 0.86f
                        scaleX = scale
                        scaleY = scale
                    },
                )
            }
        }
    }
}

/** AM over PM, sized to the wheels beside it so the whole row reads as one control. */
@Composable
private fun AmPmSelector(isAm: Boolean, onSelect: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.height(WheelItemHeight * WHEEL_VISIBLE_ITEMS),
        verticalArrangement = Arrangement.Center,
    ) {
        AmPmOption(label = "AM", selected = isAm) { onSelect(true) }
        Spacer(modifier = Modifier.height(OrbitSpacing.xs))
        AmPmOption(label = "PM", selected = !isAm) { onSelect(false) }
    }
}

@Composable
private fun AmPmOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(WheelItemHeight)
            .clip(OrbitShapes.medium)
            .background(if (selected) OrbitColors.violet600 else OrbitColors.mist, OrbitShapes.medium)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = OrbitTypography.label,
            color = if (selected) OrbitColors.cream50 else OrbitColors.slate500,
        )
    }
}

