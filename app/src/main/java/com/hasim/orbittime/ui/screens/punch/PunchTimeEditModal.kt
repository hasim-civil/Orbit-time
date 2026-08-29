package com.hasim.orbittime.ui.screens.punch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasim.orbittime.data.attendance.AttendanceLocation
import com.hasim.orbittime.ui.theme.InstrumentSerif
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import com.hasim.orbittime.util.AttendanceTimeFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val ModalCardShape = OrbitShapes.card
private val FieldShape = OrbitShapes.medium
private val TimeBoxWidth = 68.dp
private val TimeBoxHeight = 64.dp
private val TimeDigitStyle = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = 28.sp, textAlign = TextAlign.Center)

/** Off-white rounded card matching the reference's editOpen/pastOpen sheet (28dp radius,
 * rgba(255,253,249,.96) fill, 22dp padding, deep drop shadow) — the shared shell for both the
 * Edit Time and Add Past Attendance modals. */
@Composable
private fun PunchModalCard(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.cream50.copy(alpha = 0.97f), ModalCardShape)
            .padding(22.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = OrbitTypography.headline, color = OrbitColors.ink900)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, style = OrbitTypography.bodySmall, color = OrbitColors.slate600)
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(OrbitColors.mist, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "✕", style = OrbitTypography.bodyMedium, color = OrbitColors.slate600)
                }
            }
            Spacer(modifier = Modifier.height(OrbitSpacing.lg))
            Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) { content() }
        }
    }
}

private enum class TimeField { HOUR, MINUTE }

/** One "CHECK IN" / "CHECK OUT" row: a large Hour box, colon, large Minute box and a vertical
 * AM/PM selector, each labelled below — the reference's simple single text input is deliberately
 * not used here. */
@Composable
private fun TimeFieldRow(
    label: String,
    hourText: String,
    minuteText: String,
    isAm: Boolean,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit,
    onAmPmChange: (Boolean) -> Unit,
) {
    var focusedField by remember { mutableStateOf<TimeField?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.mist, FieldShape)
            .padding(OrbitSpacing.md),
    ) {
        Text(text = label, style = OrbitTypography.label, color = OrbitColors.slate500)
        Spacer(modifier = Modifier.height(OrbitSpacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimeDigitField(
                    value = hourText,
                    onValueChange = { onHourChange(it.filter(Char::isDigit).take(2)) },
                    isFocused = focusedField == TimeField.HOUR,
                    onFocusChanged = { focused -> focusedField = if (focused) TimeField.HOUR else focusedField.takeUnless { it == TimeField.HOUR } },
                )
                Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
                Text(text = "Hour", style = OrbitTypography.bodySmall, color = OrbitColors.slate500)
            }
            Text(text = ":", style = TimeDigitStyle.copy(fontSize = 30.sp), color = OrbitColors.ink900)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimeDigitField(
                    value = minuteText,
                    onValueChange = { onMinuteChange(it.filter(Char::isDigit).take(2)) },
                    isFocused = focusedField == TimeField.MINUTE,
                    onFocusChanged = { focused -> focusedField = if (focused) TimeField.MINUTE else focusedField.takeUnless { it == TimeField.MINUTE } },
                )
                Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
                Text(text = "Minute", style = OrbitTypography.bodySmall, color = OrbitColors.slate500)
            }
            Spacer(modifier = Modifier.width(OrbitSpacing.xs))
            VerticalAmPmSelector(isAm = isAm, onSelect = onAmPmChange)
        }
    }
}

@Composable
private fun TimeDigitField(
    value: String,
    onValueChange: (String) -> Unit,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(focused) { onFocusChanged(focused) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .width(TimeBoxWidth)
            .height(TimeBoxHeight)
            .background(OrbitColors.cream50, FieldShape)
            .border(1.5.dp, if (isFocused) OrbitColors.violet600 else OrbitColors.slate200, FieldShape),
        textStyle = TimeDigitStyle.copy(color = OrbitColors.ink900),
        singleLine = true,
        cursorBrush = SolidColor(OrbitColors.violet600),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        interactionSource = interactionSource,
        decorationBox = { inner ->
            Box(modifier = Modifier.width(TimeBoxWidth).height(TimeBoxHeight), contentAlignment = Alignment.Center) { inner() }
        },
    )
}

/** Vertically stacked AM / PM selector with a divider, matching the reference proportions. */
@Composable
private fun VerticalAmPmSelector(isAm: Boolean, onSelect: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .width(52.dp)
            .height(TimeBoxHeight)
            .background(OrbitColors.cream50, FieldShape)
            .border(1.dp, OrbitColors.slate200, FieldShape),
    ) {
        AmPmOption(label = "AM", selected = isAm, modifier = Modifier.weight(1f)) { onSelect(true) }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(OrbitColors.slate200))
        AmPmOption(label = "PM", selected = !isAm, modifier = Modifier.weight(1f)) { onSelect(false) }
    }
}

@Composable
private fun AmPmOption(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) OrbitColors.violet600 else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = OrbitTypography.label,
            color = if (selected) OrbitColors.cream50 else OrbitColors.slate500,
        )
    }
}

@Composable
private fun LocationPillRow(selected: AttendanceLocation?, onSelected: (AttendanceLocation) -> Unit) {
    Column {
        Text(text = "LOCATION", style = OrbitTypography.label, color = OrbitColors.slate500)
        Spacer(modifier = Modifier.height(OrbitSpacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
            AttendanceLocation.entries.forEach { location ->
                val isSelected = location == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) OrbitColors.violet600 else OrbitColors.mist, OrbitShapes.pill)
                        .clickable { onSelected(location) }
                        .padding(vertical = OrbitSpacing.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = location.label,
                        style = OrbitTypography.bodySmall,
                        color = if (isSelected) OrbitColors.cream50 else OrbitColors.slate600,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModalActionRow(cancelLabel: String, confirmLabel: String, confirmEnabled: Boolean, confirmBackground: Color, confirmContent: Color, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm), modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(OrbitColors.mist, FieldShape)
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = cancelLabel, style = OrbitTypography.titleMedium, color = OrbitColors.slate600)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(if (confirmEnabled) confirmBackground else OrbitColors.slate200, FieldShape)
                .clickable(enabled = confirmEnabled, onClick = onConfirm),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = confirmLabel, style = OrbitTypography.titleMedium, color = if (confirmEnabled) confirmContent else OrbitColors.slate500)
        }
    }
}

/** A single full-width primary action — used by [AddPastAttendanceModal], which (unlike
 * [EditTimeModal]) has no separate Cancel button since the modal's own X close already covers
 * "dismiss without saving". */
@Composable
private fun SingleActionRow(label: String, enabled: Boolean, background: Color, content: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(if (enabled) background else OrbitColors.slate200, FieldShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = OrbitTypography.titleMedium, color = if (enabled) content else OrbitColors.slate500)
    }
}

/** Small calendar glyph for the date row — a local icon rather than importing one from another
 * screen's file, matching this app's existing convention of each screen owning its own icons. */
@Composable
private fun CalendarGlyph() {
    Canvas(modifier = Modifier.size(15.dp)) {
        val scale = size.minDimension / 20f
        fun px(v: Float) = v * scale
        drawRoundRect(
            color = OrbitColors.violet600,
            topLeft = Offset(px(3f), px(4.5f)),
            size = androidx.compose.ui.geometry.Size(px(14f), px(12f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(px(3f), px(3f)),
            style = Stroke(width = px(1.6f)),
        )
        drawLine(OrbitColors.violet600, Offset(px(3f), px(8.5f)), Offset(px(17f), px(8.5f)), strokeWidth = px(1.6f))
        drawLine(OrbitColors.violet600, Offset(px(7f), px(3f)), Offset(px(7f), px(6f)), strokeWidth = px(1.6f))
        drawLine(OrbitColors.violet600, Offset(px(13f), px(3f)), Offset(px(13f), px(6f)), strokeWidth = px(1.6f))
    }
}

private fun to24Hour(hour12: Int, isAm: Boolean): Int = when {
    hour12 == 12 && isAm -> 0
    hour12 == 12 && !isAm -> 12
    isAm -> hour12
    else -> hour12 + 12
}

private fun LocalTime.to12HourParts(): Triple<Int, Int, Boolean> {
    val isAm = hour < 12
    val hour12 = when (val h = hour % 12) { 0 -> 12; else -> h }
    return Triple(hour12, minute, isAm)
}

private fun parseTimeInputs(hourText: String, minuteText: String, isAm: Boolean): LocalTime? {
    val hour12 = hourText.toIntOrNull()?.coerceIn(1, 12) ?: return null
    val minute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: return null
    return LocalTime.of(to24Hour(hour12, isAm), minute)
}

/** "Edit today's time" — the reference's editOpen sheet, rebuilt with the Hour/Minute/AM-PM
 * controls this app uses instead of its single text input.
 *
 * [initialCheckOut] is null while the user is still checked in (no real check-out time exists
 * yet) — the Check out row is hidden entirely rather than prefilled with a fake "now" value, so
 * there is never a checkout to accidentally save before the user has actually punched out.
 */
@Composable
fun EditTimeModal(
    initialCheckIn: LocalTime,
    initialCheckOut: LocalTime?,
    onConfirm: (LocalTime, LocalTime?, AttendanceLocation?) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Edit today's time",
    subtitle: String = "Type today's punch times and pick AM or PM.",
    initialLocation: AttendanceLocation? = null,
    // Timesheet reuses this modal for a genuinely past Daily History day, which — unlike
    // "today, possibly still checked in" — always has a real (or addable) check-out, so the
    // field should never be hidden there even if that day's check-out hasn't been set yet.
    forceShowCheckOut: Boolean = false,
) {
    val hasCheckOut = initialCheckOut != null || forceShowCheckOut
    val (inHour0, inMinute0, inAm0) = initialCheckIn.to12HourParts()
    val (outHour0, outMinute0, outAm0) = (initialCheckOut ?: LocalTime.NOON).to12HourParts()
    var inHour by remember { mutableStateOf(inHour0.toString()) }
    var inMinute by remember { mutableStateOf(inMinute0.toString().padStart(2, '0')) }
    var inAm by remember { mutableStateOf(inAm0) }
    var outHour by remember { mutableStateOf(outHour0.toString()) }
    var outMinute by remember { mutableStateOf(outMinute0.toString().padStart(2, '0')) }
    var outAm by remember { mutableStateOf(outAm0) }
    var location by remember { mutableStateOf(initialLocation) }

    val checkIn = parseTimeInputs(inHour, inMinute, inAm)
    val checkOut = if (hasCheckOut) parseTimeInputs(outHour, outMinute, outAm) else null
    val valid = checkIn != null && (!hasCheckOut || checkOut != null)

    PunchModalCard(title = title, subtitle = subtitle, onDismiss = onDismiss) {
        TimeFieldRow("CHECK IN", inHour, inMinute, inAm, { inHour = it }, { inMinute = it }, { inAm = it })
        if (hasCheckOut) {
            TimeFieldRow("CHECK OUT", outHour, outMinute, outAm, { outHour = it }, { outMinute = it }, { outAm = it })
        }
        LocationPillRow(selected = location, onSelected = { location = it })
        ModalActionRow(
            cancelLabel = "Cancel",
            confirmLabel = "Save",
            confirmEnabled = valid,
            confirmBackground = OrbitColors.violet600,
            confirmContent = OrbitColors.cream50,
            onCancel = onDismiss,
            onConfirm = { if (checkIn != null && (!hasCheckOut || checkOut != null)) onConfirm(checkIn, checkOut, location) },
        )
    }
}

/** "Add past attendance" — the reference's pastOpen sheet, same Hour/Minute/AM-PM and location
 * controls as [EditTimeModal], plus a date picker row and a live total. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPastAttendanceModal(
    initialDate: LocalDate,
    onConfirm: (LocalDate, LocalTime, LocalTime, AttendanceLocation?) -> Unit,
    onDismiss: () -> Unit,
    defaultCheckIn: LocalTime = LocalTime.of(9, 0),
    defaultCheckOut: LocalTime = LocalTime.of(17, 30),
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    val (defaultInHour, defaultInMinute, defaultInAm) = defaultCheckIn.to12HourParts()
    val (defaultOutHour, defaultOutMinute, defaultOutAm) = defaultCheckOut.to12HourParts()
    var inHour by remember { mutableStateOf(defaultInHour.toString()) }
    var inMinute by remember { mutableStateOf(defaultInMinute.toString().padStart(2, '0')) }
    var inAm by remember { mutableStateOf(defaultInAm) }
    var outHour by remember { mutableStateOf(defaultOutHour.toString()) }
    var outMinute by remember { mutableStateOf(defaultOutMinute.toString().padStart(2, '0')) }
    var outAm by remember { mutableStateOf(defaultOutAm) }
    var location by remember { mutableStateOf<AttendanceLocation?>(null) }

    val checkIn = parseTimeInputs(inHour, inMinute, inAm)
    val checkOut = parseTimeInputs(outHour, outMinute, outAm)
    val valid = checkIn != null && checkOut != null
    val totalLabel = if (checkIn != null && checkOut != null) {
        val minutes = Duration.between(checkIn, checkOut).let { if (it.isNegative) it.plusHours(24) else it }.toMinutes()
        "${minutes / 60}h ${minutes % 60}m"
    } else {
        "—"
    }

    PunchModalCard(
        title = "Add past attendance",
        subtitle = "Log a shift you forgot to punch.",
        onDismiss = onDismiss,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrbitColors.mist, FieldShape)
                .clickable { showDatePicker = true }
                .padding(horizontal = OrbitSpacing.md, vertical = OrbitSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                CalendarGlyph()
                Text(text = "Select date", style = OrbitTypography.bodyMedium, color = OrbitColors.slate600)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = AttendanceTimeFormat.dayLabel(selectedDate), style = OrbitTypography.titleMedium, color = OrbitColors.ink900)
                Text(text = "›", style = OrbitTypography.titleMedium, color = OrbitColors.violet600)
            }
        }
        TimeFieldRow("CHECK IN", inHour, inMinute, inAm, { inHour = it }, { inMinute = it }, { inAm = it })
        TimeFieldRow("CHECK OUT", outHour, outMinute, outAm, { outHour = it }, { outMinute = it }, { outAm = it })
        LocationPillRow(selected = location, onSelected = { location = it })
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Total", style = OrbitTypography.bodySmall, color = OrbitColors.slate600)
            Text(text = totalLabel, style = OrbitTypography.titleMedium, color = OrbitColors.ink900)
        }
        SingleActionRow(
            label = "Add entry",
            enabled = valid,
            background = OrbitColors.violet600,
            content = OrbitColors.cream50,
            onClick = { if (checkIn != null && checkOut != null) onConfirm(selectedDate, checkIn, checkOut, location) },
        )
    }

    if (showDatePicker) {
        val todayMillis = remember { AttendanceTimeFormat.today().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis < todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}
