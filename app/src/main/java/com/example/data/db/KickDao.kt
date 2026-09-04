package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.KickLog
import com.example.data.model.KickSession
import kotlinx.coroutines.flow.Flow

@Dao
interface KickDao {
    // --- Kick Logs (Single movements) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKickLog(log: KickLog): Long

    @Query("SELECT * FROM kick_logs WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getLogsForDate(dateString: String): Flow<List<KickLog>>

    @Query("SELECT COUNT(*) FROM kick_logs WHERE dateString = :dateString")
    fun getCountForDate(dateString: String): Flow<Int>

    @Query("SELECT * FROM kick_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLog(): Flow<KickLog?>

    @Query("SELECT * FROM kick_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getLogsForSession(sessionId: Long): Flow<List<KickLog>>

    @Query("SELECT * FROM kick_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<KickLog>>

    @Query("DELETE FROM kick_logs WHERE id = :id")
    suspend fun deleteKickLogById(id: Long)

    @Query("DELETE FROM kick_logs WHERE id = (SELECT id FROM kick_logs ORDER BY timestamp DESC LIMIT 1)")
    suspend fun deleteLatestKickLog()

    // --- Kick Sessions (Count to 10) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: KickSession): Long

    @Update
    suspend fun updateSession(session: KickSession)

    @Query("SELECT * FROM kick_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): KickSession?

    @Query("SELECT * FROM kick_sessions WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): KickSession?

    @Query("SELECT * FROM kick_sessions WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    fun getActiveSessionFlow(): Flow<KickSession?>

    @Query("SELECT * FROM kick_sessions WHERE dateString = :dateString ORDER BY startTime DESC")
    fun getSessionsForDate(dateString: String): Flow<List<KickSession>>

    @Query("SELECT * FROM kick_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<KickSession>>

    @Query("DELETE FROM kick_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
}
