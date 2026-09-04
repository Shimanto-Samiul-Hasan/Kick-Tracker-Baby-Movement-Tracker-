package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a structured "Count to 10" session, tracking how long
 * it takes to feel 10 movements according to standard obstetric guidelines.
 */
@Entity(tableName = "kick_sessions")
data class KickSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val targetCount: Int = 10,
    val completedCount: Int = 0,
    val isCompleted: Boolean = false,
    val durationSeconds: Long = 0L,
    val dateString: String, // Format: yyyy-MM-dd
    val notes: String = ""
)
