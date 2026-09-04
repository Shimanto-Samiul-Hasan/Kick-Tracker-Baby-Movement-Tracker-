package com.example.data.repository

import com.example.data.db.KickDao
import com.example.data.model.KickLog
import com.example.data.model.KickSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class KickRepository(private val kickDao: KickDao) {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getTodayDateString(): String {
        return LocalDate.now().format(dateFormatter)
    }

    fun getTodayLogs(): Flow<List<KickLog>> =
        kickDao.getLogsForDate(getTodayDateString())

    fun getTodayCount(): Flow<Int> =
        kickDao.getCountForDate(getTodayDateString())

    fun getLatestLog(): Flow<KickLog?> =
        kickDao.getLatestLog()

    fun getActiveSessionFlow(): Flow<KickSession?> =
        kickDao.getActiveSessionFlow()

    fun getAllSessions(): Flow<List<KickSession>> =
        kickDao.getAllSessions()

    fun getAllLogs(): Flow<List<KickLog>> =
        kickDao.getAllLogs()

    fun getLogsForDate(dateString: String): Flow<List<KickLog>> =
        kickDao.getLogsForDate(dateString)

    fun getSessionsForDate(dateString: String): Flow<List<KickSession>> =
        kickDao.getSessionsForDate(dateString)

    suspend fun logCasualKick(movementType: String = "Kick", note: String = ""): Long {
        val now = System.currentTimeMillis()
        val log = KickLog(
            timestamp = now,
            dateString = getTodayDateString(),
            sessionId = null,
            movementType = movementType,
            note = note
        )
        return kickDao.insertKickLog(log)
    }

    suspend fun startCountToTenSession(targetCount: Int = 10): Long {
        // Cancel any pending uncompleted session first to maintain single active session invariant
        val existingActive = kickDao.getActiveSession()
        if (existingActive != null) {
            kickDao.deleteSessionById(existingActive.id)
        }

        val session = KickSession(
            startTime = System.currentTimeMillis(),
            endTime = null,
            targetCount = targetCount,
            completedCount = 0,
            isCompleted = false,
            durationSeconds = 0L,
            dateString = getTodayDateString()
        )
        return kickDao.insertSession(session)
    }

    suspend fun logSessionKick(sessionId: Long, movementType: String = "Kick"): KickSession? {
        val session = kickDao.getSessionById(sessionId) ?: return null
        val now = System.currentTimeMillis()

        // Insert movement log with link to session
        val log = KickLog(
            timestamp = now,
            dateString = getTodayDateString(),
            sessionId = sessionId,
            movementType = movementType
        )
        kickDao.insertKickLog(log)

        val newCount = session.completedCount + 1
        val elapsedSec = (now - session.startTime) / 1000
        val isCompleted = newCount >= session.targetCount

        val updatedSession = session.copy(
            completedCount = newCount,
            durationSeconds = elapsedSec,
            isCompleted = isCompleted,
            endTime = if (isCompleted) now else null
        )
        kickDao.updateSession(updatedSession)
        return updatedSession
    }

    suspend fun cancelActiveSession(sessionId: Long) {
        kickDao.deleteSessionById(sessionId)
    }

    suspend fun deleteLatestKick() {
        kickDao.deleteLatestKickLog()
    }

    suspend fun deleteKickLog(id: Long) {
        kickDao.deleteKickLogById(id)
    }

    suspend fun deleteSession(sessionId: Long) {
        kickDao.deleteSessionById(sessionId)
    }
}
