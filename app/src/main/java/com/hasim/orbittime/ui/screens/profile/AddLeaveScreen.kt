package com.hasim.orbittime.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.data.leave.LeaveRecord
import com.hasim.orbittime.data.leave.LeaveType
import com.hasim.orbittime.ui.components.AuthScreenScaffold
import com.hasim.orbittime.ui.components.OrbitGradientButton
import com.hasim.orbittime.ui.components.OrbitTextField
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import com.hasim.orbittime.util.AttendanceTimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun AddLeaveScreen(
    onBackClick: () -> Unit,
    viewModel: LeaveViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingLeave by remember { mutableStateOf<LeaveRecord?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    AuthScreenScaffold(
        headline = "Add leave",
        subtitle = "Track your own leave — no approval needed.",
        onBackClick = onBackClick,
        footer = {
            OrbitGradientButton(
                text = "+  Add leave",
                onClick = {
                    editingLeave = null
                    showEditor = true
                },
            )
        },
    ) {
        if (uiState.leaves.isEmpty() && !uiState.isLoading) {
            Text(
                text = "No leave records yet.",
                style = OrbitTypography.bodyMedium,
                color = OrbitColors.slate600,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = OrbitSpacing.xxl),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                uiState.leaves.forEach { leave ->
                    LeaveRow(
                        leave = leave,
                        onClick = {
                            editingLeave = leave
                            showEditor = true
                        },
                    )
                }
            }
        }
    }

    if (showEditor) {
        LeaveEditorDialog(
            initial = editingLeave,
            onSave = { leave ->
                viewModel.saveLeave(leave)
                showEditor = false
            },
            onDelete = editingLeave?.let { leave ->
                {
                    viewModel.deleteLeave(leave.id)
                    showEditor = false
                }
            },
            onDismiss = { showEditor = false },
        )
    }
}

private fun LeaveRecord.rangeLabel(): String {
    val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return startDate
    val end = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: start
    return if (start == end) AttendanceTimeFormat.dayLabel(start) else "${AttendanceTimeFormat.dayLabel(start)} – ${AttendanceTimeFormat.dayLabel(end)}"
}

private fun LeaveRecord.typeLabel(): String =
    runCatching { LeaveType.valueOf(type) }.getOrNull()?.label ?: type

@Composable
private fun LeaveRow(leave: LeaveRecord, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.cream50, OrbitShapes.medium)
            .clickable(onClick = onClick)
            .padding(OrbitSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = leave.typeLabel(), style = OrbitTypography.titleMedium, color = OrbitColors.ink900)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = leave.rangeLabel(), style = OrbitTypography.bodySmall, color = OrbitColors.slate600)
            if (leave.note.isNotBlank()) {
                Text(text = leave.note, style = OrbitTypography.bodySmall, color = OrbitColors.slate500)
            }
        }
        Text(text = "›", style = OrbitTypography.titleMedium, color = OrbitColors.slate300)
    }
}

@Composable
private fun LeaveTypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = OrbitTypography.bodySmall,
        color = if (selected) OrbitColors.cream50 else OrbitColors.ink900,
        modifier = Modifier
            .wrapContentWidth()
            .background(if (selected) OrbitColors.violet600 else OrbitColors.mist, OrbitShapes.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = OrbitSpacing.md, vertical = OrbitSpacing.sm),
    )
}

private enum class LeaveDateField { START, END }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaveEditorDialog(
    initial: LeaveRecord?,
    onSave: (LeaveRecord) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var type by remember {
        mutableStateOf(runCatching { LeaveType.valueOf(initial?.type ?: LeaveType.CASUAL.name) }.getOrDefault(LeaveType.CASUAL))
    }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var startDate by remember {
        mutableStateOf(initial?.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: AttendanceTimeFormat.today())
    }
    var endDate by remember {
        mutableStateOf(initial?.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: startDate)
    }
    var datePickerField by remember { mutableStateOf<LeaveDateField?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (initial == null) "Add leave" else "Edit leave", style = OrbitTypography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                Text(text = "LEAVE TYPE", style = OrbitTypography.label, color = OrbitColors.slate500)
                Spacer(modifier = Modifier.height(OrbitSpacing.xs))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
                ) {
                    LeaveType.entries.forEach { option ->
                        LeaveTypeChip(label = option.label, selected = option == type, onClick = { type = option })
                    }
                }
                Spacer(modifier = Modifier.height(OrbitSpacing.sm))
                Text(text = "FROM", style = OrbitTypography.label, color = OrbitColors.slate500)
                Spacer(modifier = Modifier.height(OrbitSpacing.xs))
                DateRow(date = startDate, onClick = { datePickerField = LeaveDateField.START })
                Spacer(modifier = Modifier.height(OrbitSpacing.sm))
                Text(text = "TO", style = OrbitTypography.label, color = OrbitColors.slate500)
                Spacer(modifier = Modifier.height(OrbitSpacing.xs))
                DateRow(date = endDate, onClick = { datePickerField = LeaveDateField.END })
                Spacer(modifier = Modifier.height(OrbitSpacing.sm))
                OrbitTextField(label = "NOTE (OPTIONAL)", value = note, onValueChange = { note = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val orderedEnd = if (endDate.isBefore(startDate)) startDate else endDate
                onSave(
                    LeaveRecord(
                        id = initial?.id.orEmpty(),
                        type = type.name,
                        startDate = AttendanceTimeFormat.dateKey(startDate),
                        endDate = AttendanceTimeFormat.dateKey(orderedEnd),
                        note = note.trim(),
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = OrbitColors.danger) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    val activeField = datePickerField
    if (activeField != null) {
        val currentValue = if (activeField == LeaveDateField.START) startDate else endDate
        val state = rememberDatePickerState(
            initialSelectedDateMillis = currentValue.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { datePickerField = null },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        if (activeField == LeaveDateField.START) {
                            startDate = picked
                            if (endDate.isBefore(picked)) endDate = picked
                        } else {
                            endDate = picked
                        }
                    }
                    datePickerField = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerField = null }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun DateRow(date: LocalDate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.mist, OrbitShapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = OrbitSpacing.md, vertical = OrbitSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = AttendanceTimeFormat.dayLabel(date), style = OrbitTypography.bodyMedium, color = OrbitColors.ink900)
        Text(text = "⌄", style = OrbitTypography.titleMedium, color = OrbitColors.slate500)
    }
}
