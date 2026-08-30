package com.hasim.orbittime.ui.screens.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.attendance.AttendanceRepository
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.leave.LeaveRepository
import com.hasim.orbittime.data.user.UserProfileRepository
import com.hasim.orbittime.util.AttendanceRangeMode
import com.hasim.orbittime.util.AttendanceStats
import com.hasim.orbittime.util.AttendanceTimeFormat
import com.hasim.orbittime.util.DailyAttendance
import com.hasim.orbittime.util.ReportsPerformance
import com.hasim.orbittime.util.ReportsPunctuality
import com.hasim.orbittime.util.ReportsStats
import com.hasim.orbittime.util.ReportsTrendPoint
import com.hasim.orbittime.util.ReportsWorkHours
import com.hasim.orbittime.util.observeIsOnline
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How many trailing weekly/monthly buckets the trend chart shows — kept small on purpose
 * ("keep the chart simple and clean"), not a user-adjustable range. */
private const val TREND_BUCKET_COUNT = 6L

data class ReportsUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val errorMessage: String? = null,
    /** True once there's at least one real check-in anywhere in the fetched range — before that,
     * the screen shows "No attendance data yet" instead of all-zero statistics. */
    val hasEnoughData: Boolean = false,
    val performance: ReportsPerformance = ReportsPerformance(),
    val workHours: ReportsWorkHours = ReportsWorkHours(),
    val punctuality: ReportsPunctuality = ReportsPunctuality(),
    val trendMode: AttendanceRangeMode = AttendanceRangeMode.WEEK,
    val trendPoints: List<ReportsTrendPoint> = emptyList(),
)

/**
 * Backs the Reports tab. Reports is read-only analysis over the SAME attendance data Home and
 * Timesheet already read from [AttendanceRepository] — this view model creates no new Firestore
 * collection, writes nothing back, and performs exactly one [AttendanceRepository.observeRange]
 * listener (covering the widest window any section here needs), reusing that one dataset for
 * every statistic and both trend granularities instead of querying once per figure.
 */
class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val attendanceRepository = AttendanceRepository()
    private val profileRepository = UserProfileRepository()
    private val leaveRepository = LeaveRepository()

    private val today = AttendanceTimeFormat.today()

    /** Covers the current + previous month (for the vs-last-month comparison) and the last
     * [TREND_BUCKET_COUNT] weeks/months (for the trend chart) in one range — six calendar months
     * back is comfortably wider than six weeks, so one query serves both granularities. */
    private val fetchStart = today.minusMonths(TREND_BUCKET_COUNT - 1).withDayOfMonth(1)

    private var rangeRecords: Map<LocalDate, DailyAttendance> = emptyMap()
    private var leaveDates: Set<LocalDate> = emptySet()
    private var lateAfter: LocalTime = AttendanceStats.DEFAULT_LATE_AFTER

    private var rangeJob: Job? = null

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "You're not signed in.") }
        } else {
            observeConnectivity()
            observeRange(uid)
            observeLeaves(uid)
            loadShiftStart(uid)
        }
    }

    private fun observeRange(uid: String) {
        rangeJob?.cancel()
        rangeJob = viewModelScope.launch {
            attendanceRepository.observeRange(uid, AttendanceTimeFormat.dateKey(fetchStart), AttendanceTimeFormat.dateKey(today))
                .catch { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
                .collect { records ->
                    rangeRecords = records.mapNotNull { record ->
                        val date = runCatching { LocalDate.parse(record.date) }.getOrNull() ?: return@mapNotNull null
                        date to DailyAttendance(
                            date = date,
                            checkInAt = record.checkInAt?.toDate()?.toInstant(),
                            checkOutAt = record.checkOutAt?.toDate()?.toInstant(),
                        )
                    }.toMap()
                    recompute()
                }
        }
    }

    private fun observeLeaves(uid: String) {
        viewModelScope.launch {
            leaveRepository.observeLeaves(uid)
                .catch { /* Leave dates are an enhancement to the figures above; a failure here shouldn't block Reports. */ }
                .collect { leaves ->
                    leaveDates = leaves.flatMap { it.dateRange() }.toSet()
                    recompute()
                }
        }
    }

    private fun loadShiftStart(uid: String) {
        viewModelScope.launch {
            val profile = runCatching { profileRepository.getProfile(uid) }.getOrNull()
            val parsed = profile?.shiftStart?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            if (parsed != null) {
                lateAfter = parsed
                recompute()
            }
        }
    }

    fun setTrendMode(mode: AttendanceRangeMode) {
        _uiState.update { it.copy(trendMode = mode, trendPoints = trendPoints(mode)) }
    }

    /** Manual retry only — never an automatic retry loop. */
    fun retry() {
        val uid = authRepository.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        observeRange(uid)
    }

    private fun recompute() {
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
        val previousMonthEnd = monthStart.minusDays(1)
        val previousMonthStart = previousMonthEnd.withDayOfMonth(1)

        val performance = ReportsStats.performance(
            records = rangeRecords,
            currentStart = monthStart, currentEnd = monthEnd,
            previousStart = previousMonthStart, previousEnd = previousMonthEnd,
            today = today, lateAfter = lateAfter, leaveDates = leaveDates,
        )
        val workHours = ReportsStats.workHours(rangeRecords, monthStart, monthEnd, today, Instant.now())
        val punctuality = ReportsStats.punctuality(rangeRecords, monthStart, monthEnd, today, lateAfter, leaveDates)
        val hasEnoughData = rangeRecords.values.any { it.checkInAt != null }

        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                hasEnoughData = hasEnoughData,
                performance = performance,
                workHours = workHours,
                punctuality = punctuality,
                trendPoints = trendPoints(it.trendMode),
            )
        }
    }

    private fun trendPoints(mode: AttendanceRangeMode): List<ReportsTrendPoint> = when (mode) {
        AttendanceRangeMode.WEEK -> (TREND_BUCKET_COUNT - 1 downTo 0).map { weeksAgo ->
            val anchor = today.minusWeeks(weeksAgo)
            val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = anchor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            val summary = AttendanceStats.summarize(rangeRecords, start, end, today, "", lateAfter = lateAfter, leaveDates = leaveDates)
            ReportsTrendPoint(label = "${start.dayOfMonth}/${start.monthValue}", attendanceRatePercent = summary.attendanceRatePercent)
        }
        AttendanceRangeMode.MONTH -> (TREND_BUCKET_COUNT - 1 downTo 0).map { monthsAgo ->
            val monthDate = today.minusMonths(monthsAgo)
            val start = monthDate.withDayOfMonth(1)
            val end = monthDate.withDayOfMonth(monthDate.lengthOfMonth())
            val summary = AttendanceStats.summarize(rangeRecords, start, end, today, "", lateAfter = lateAfter, leaveDates = leaveDates)
            val label = monthDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            ReportsTrendPoint(label = label, attendanceRatePercent = summary.attendanceRatePercent)
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            observeIsOnline(getApplication()).collect { online -> _uiState.update { it.copy(isOnline = online) } }
        }
    }
}
