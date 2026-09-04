package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.KickLog
import com.example.data.model.KickSession
import com.example.data.repository.KickRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class AppMode {
    QUICK_LOG,
    COUNT_TO_TEN
}

data class KickTrackerUiState(
    val todayCount: Int = 0,
    val todayLogs: List<KickLog> = emptyList(),
    val latestLog: KickLog? = null,
    val activeSession: KickSession? = null,
    val allSessions: List<KickSession> = emptyList(),
    val allLogs: List<KickLog> = emptyList(),
    val selectedMode: AppMode = AppMode.QUICK_LOG,
    val isAmoledDark: Boolean = true, // Default to true AMOLED black for nighttime ease
    val hapticEnabled: Boolean = true,
    val showOnboarding: Boolean = false,
    val sessionElapsedSeconds: Long = 0L,
    val selectedMovementType: String = "Kick",
    val sessionCompletedBanner: KickSession? = null,
    val statusMessage: String? = null
)

class KickTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KickRepository = KickRepository(
        AppDatabase.getDatabase(application).kickDao()
    )

    private val _selectedMode = MutableStateFlow(AppMode.QUICK_LOG)
    private val _isAmoledDark = MutableStateFlow(true)
    private val _hapticEnabled = MutableStateFlow(true)
    private val _showOnboarding = MutableStateFlow(false)
    private val _sessionElapsedSeconds = MutableStateFlow(0L)
    private val _selectedMovementType = MutableStateFlow("Kick")
    private val _sessionCompletedBanner = MutableStateFlow<KickSession?>(null)
    private val _statusMessage = MutableStateFlow<String?>(null)

    private var timerJob: Job? = null

    init {
        // Check if first run for onboarding
        val prefs = application.getSharedPreferences("kick_tracker_prefs", Application.MODE_PRIVATE)
        val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding_v1", false)
        if (!hasSeenOnboarding) {
            _showOnboarding.value = true
        }

        // Start active session observer to manage elapsed timer
        viewModelScope.launch {
            repository.getActiveSessionFlow().collect { session ->
                if (session != null) {
                    startTimer(session.startTime)
                } else {
                    stopTimer()
                }
            }
        }
    }

    private fun startTimer(startTime: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(0) / 1000
                _sessionElapsedSeconds.value = elapsed
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _sessionElapsedSeconds.value = 0L
    }

    // Combine database flows and state flows into a single unified reactive UI state
    val uiState: StateFlow<KickTrackerUiState> = combine(
        repository.getTodayCount(),
        repository.getTodayLogs(),
        repository.getLatestLog(),
        repository.getActiveSessionFlow(),
        repository.getAllSessions(),
        repository.getAllLogs(),
        _selectedMode,
        _isAmoledDark,
        _hapticEnabled,
        _showOnboarding,
        _sessionElapsedSeconds,
        _selectedMovementType,
        _sessionCompletedBanner,
        _statusMessage
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        KickTrackerUiState(
            todayCount = args[0] as Int,
            todayLogs = args[1] as List<KickLog>,
            latestLog = args[2] as? KickLog,
            activeSession = args[3] as? KickSession,
            allSessions = args[4] as List<KickSession>,
            allLogs = args[5] as List<KickLog>,
            selectedMode = args[6] as AppMode,
            isAmoledDark = args[7] as Boolean,
            hapticEnabled = args[8] as Boolean,
            showOnboarding = args[9] as Boolean,
            sessionElapsedSeconds = args[10] as Long,
            selectedMovementType = args[11] as String,
            sessionCompletedBanner = args[12] as? KickSession,
            statusMessage = args[13] as? String
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KickTrackerUiState()
    )

    fun setMode(mode: AppMode) {
        _selectedMode.value = mode
    }

    fun setMovementType(type: String) {
        _selectedMovementType.value = type
    }

    fun toggleAmoledDark() {
        _isAmoledDark.value = !_isAmoledDark.value
    }

    fun toggleHaptic() {
        _hapticEnabled.value = !_hapticEnabled.value
    }

    fun dismissOnboarding() {
        _showOnboarding.value = false
        val prefs = getApplication<Application>().getSharedPreferences("kick_tracker_prefs", Application.MODE_PRIVATE)
        prefs.edit().putBoolean("has_seen_onboarding_v1", true).apply()
    }

    fun showOnboardingManual() {
        _showOnboarding.value = true
    }

    fun dismissCompletionBanner() {
        _sessionCompletedBanner.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Primary action triggered by the massive center button.
     * Logs either to casual day log or increments the active Count to 10 session.
     */
    fun logMovement() {
        viewModelScope.launch {
            val currentState = uiState.value
            val currentType = _selectedMovementType.value

            if (currentState.selectedMode == AppMode.COUNT_TO_TEN) {
                val activeSession = currentState.activeSession
                if (activeSession == null) {
                    // Start a new Count to 10 session automatically if not yet active
                    val sessionId = repository.startCountToTenSession(targetCount = 10)
                    val updated = repository.logSessionKick(sessionId, currentType)
                    if (updated != null && updated.isCompleted) {
                        _sessionCompletedBanner.value = updated
                    }
                } else {
                    val updated = repository.logSessionKick(activeSession.id, currentType)
                    if (updated != null && updated.isCompleted) {
                        _sessionCompletedBanner.value = updated
                    }
                }
            } else {
                repository.logCasualKick(currentType)
            }
        }
    }

    fun startCountToTen() {
        viewModelScope.launch {
            _selectedMode.value = AppMode.COUNT_TO_TEN
            repository.startCountToTenSession(targetCount = 10)
        }
    }

    fun cancelCountToTen() {
        viewModelScope.launch {
            val active = uiState.value.activeSession
            if (active != null) {
                repository.cancelActiveSession(active.id)
                _statusMessage.value = "Session reset"
            }
        }
    }

    fun undoLastKick() {
        viewModelScope.launch {
            repository.deleteLatestKick()
            _statusMessage.value = "Last movement removed"
        }
    }

    fun deleteKickLog(id: Long) {
        viewModelScope.launch {
            repository.deleteKickLog(id)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    /**
     * Formats a clinical medical summary report for obstetrician review.
     */
    fun generateMedicalReport(patientName: String = "Patient"): String {
        val state = uiState.value
        val allLogs = state.allLogs
        val allSessions = state.allSessions

        val totalKicks = allLogs.size
        val groupedByDate = allLogs.groupBy { it.dateString }
        val daysActive = groupedByDate.keys.size.coerceAtLeast(1)
        val avgKicksPerDay = totalKicks / daysActive.toFloat()

        val completedSessions = allSessions.filter { it.isCompleted }
        val avgSessionMinutes = if (completedSessions.isNotEmpty()) {
            completedSessions.map { it.durationSeconds }.average() / 60.0
        } else null

        val sb = StringBuilder()
        sb.appendLine("==========================================")
        sb.appendLine("FETAL MOVEMENT CLINICAL REPORT (KICK LOG)")
        sb.appendLine("Patient: $patientName")
        sb.appendLine("Generated: ${LocalDate.now()} | Total Days: $daysActive")
        sb.appendLine("==========================================")
        sb.appendLine("CLINICAL SUMMARY:")
        sb.appendLine("• Total movements recorded: $totalKicks")
        sb.appendLine("• Daily average movements: ${String.format("%.1f", avgKicksPerDay)}")
        if (avgSessionMinutes != null) {
            sb.appendLine("• Count-to-10 sessions completed: ${completedSessions.size}")
            sb.appendLine("• Average time to 10 kicks: ${String.format("%.1f", avgSessionMinutes)} minutes (ACOG benchmark: < 120 mins)")
        } else {
            sb.appendLine("• Count-to-10 sessions: None recorded yet")
        }
        sb.appendLine()
        sb.appendLine("CHRONOLOGICAL DAILY LOG (LAST 14 DAYS):")

        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
        val sortedDates = groupedByDate.keys.sortedDescending().take(14)

        for (dateStr in sortedDates) {
            val dayLogs = groupedByDate[dateStr] ?: emptyList()
            val daySessions = allSessions.filter { it.dateString == dateStr }

            sb.appendLine("------------------------------------------")
            sb.appendLine("Date: $dateStr | Total Movements: ${dayLogs.size}")

            if (daySessions.isNotEmpty()) {
                sb.appendLine("  [Count-to-10 Sessions]")
                for (s in daySessions) {
                    val durationMin = s.durationSeconds / 60
                    val durationSec = s.durationSeconds % 60
                    val status = if (s.isCompleted) "Completed in ${durationMin}m ${durationSec}s" else "Incomplete (${s.completedCount}/10)"
                    sb.appendLine("   - $status")
                }
            }

            sb.appendLine("  [Movement Timestamps]")
            val sampleTimestamps = dayLogs.take(15).map { log ->
                val time = Instant.ofEpochMilli(log.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(timeFormatter)
                "$time (${log.movementType})"
            }
            sb.appendLine("   " + sampleTimestamps.joinToString(", "))
            if (dayLogs.size > 15) {
                sb.appendLine("   ... and ${dayLogs.size - 15} more")
            }
        }

        sb.appendLine("==========================================")
        sb.appendLine("Report generated by Kick Tracker for Android")
        return sb.toString()
    }

    /**
     * Formats CSV export for medical records or spreadsheet analysis.
     */
    fun generateCsvExport(): String {
        val state = uiState.value
        val allLogs = state.allLogs
        val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        val sb = StringBuilder()
        sb.appendLine("LogId,Date,Timestamp,MovementType,SessionId,Note")
        for (log in allLogs) {
            val formattedTime = Instant.ofEpochMilli(log.timestamp)
                .atZone(ZoneId.systemDefault())
                .format(timeFormatter)
            sb.appendLine("${log.id},\"${log.dateString}\",\"$formattedTime\",\"${log.movementType}\",\"${log.sessionId ?: "Casual"}\",\"${log.note}\"")
        }
        return sb.toString()
    }
}
