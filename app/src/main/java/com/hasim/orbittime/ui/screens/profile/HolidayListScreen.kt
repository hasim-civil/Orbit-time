package com.hasim.orbittime.ui.screens.profile

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
import com.hasim.orbittime.data.holiday.HolidayRecord
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
fun HolidayListScreen(
    onBackClick: () -> Unit,
    viewModel: HolidayViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingHoliday by remember { mutableStateOf<HolidayRecord?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    AuthScreenScaffold(
        headline = "Holiday list",
        subtitle = "Your personal holiday calendar.",
        onBackClick = onBackClick,
        footer = {
            OrbitGradientButton(
                text = "+  Add holiday",
                onClick = {
                    editingHoliday = null
                    showEditor = true
                },
            )
        },
    ) {
        if (uiState.holidays.isEmpty() && !uiState.isLoading) {
            Text(
                text = "No holidays added yet.",
                style = OrbitTypography.bodyMedium,
                color = OrbitColors.slate600,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = OrbitSpacing.xxl),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                uiState.holidays.forEach { holiday ->
                    HolidayRow(
                        holiday = holiday,
                        onClick = {
                            editingHoliday = holiday
                            showEditor = true
                        },
                    )
                }
            }
        }
    }

    if (showEditor) {
        HolidayEditorDialog(
            initial = editingHoliday,
            onSave = { holiday ->
                viewModel.saveHoliday(holiday)
                showEditor = false
            },
            onDelete = editingHoliday?.let { holiday ->
                {
                    viewModel.deleteHoliday(holiday.id)
                    showEditor = false
                }
            },
            onDismiss = { showEditor = false },
        )
    }
}

@Composable
private fun HolidayRow(holiday: HolidayRecord, onClick: () -> Unit) {
    val dateLabel = runCatching { LocalDate.parse(holiday.date) }.getOrNull()?.let { AttendanceTimeFormat.dayLabel(it) } ?: holiday.date

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.cream50, OrbitShapes.medium)
            .clickable(onClick = onClick)
            .padding(OrbitSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = holiday.name, style = OrbitTypography.titleMedium, color = OrbitColors.ink900)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = dateLabel, style = OrbitTypography.bodySmall, color = OrbitColors.slate600)
            if (holiday.description.isNotBlank()) {
                Text(text = holiday.description, style = OrbitTypography.bodySmall, color = OrbitColors.slate500)
            }
        }
        Text(text = "›", style = OrbitTypography.titleMedium, color = OrbitColors.slate300)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HolidayEditorDialog(
    initial: HolidayRecord?,
    onSave: (HolidayRecord) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember {
        mutableStateOf(initial?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: AttendanceTimeFormat.today())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (initial == null) "Add holiday" else "Edit holiday", style = OrbitTypography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                OrbitTextField(label = "HOLIDAY NAME", value = name, onValueChange = { name = it })
                Column {
                    Text(text = "DATE", style = OrbitTypography.label, color = OrbitColors.slate500)
                    Spacer(modifier = Modifier.height(OrbitSpacing.xs))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OrbitColors.mist, OrbitShapes.medium)
                            .clickable { showDatePicker = true }
                            .padding(horizontal = OrbitSpacing.md, vertical = OrbitSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = AttendanceTimeFormat.dayLabel(selectedDate), style = OrbitTypography.bodyMedium, color = OrbitColors.ink900)
                        Text(text = "⌄", style = OrbitTypography.titleMedium, color = OrbitColors.slate500)
                    }
                }
                OrbitTextField(label = "DESCRIPTION (OPTIONAL)", value = description, onValueChange = { description = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    HolidayRecord(
                        id = initial?.id.orEmpty(),
                        name = name.trim(),
                        date = AttendanceTimeFormat.dateKey(selectedDate),
                        description = description.trim(),
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

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
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
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
