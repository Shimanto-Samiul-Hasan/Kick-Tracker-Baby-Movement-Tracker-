package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an individual timestamped fetal movement log.
 * [sessionId] is null for casual daily quick logs, or references a [KickSession]
 * when recorded during a structured "Count to 10" session.
 */
@Entity(tableName = "kick_logs")
data class KickLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String, // Format: yyyy-MM-dd
    val sessionId: Long? = null,
    val movementType: String = "Kick", // "Kick", "Flutter", "Roll", "Hiccup"
    val note: String = ""
)
