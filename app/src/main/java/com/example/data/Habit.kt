package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val category: String = "General", // "Health", "Fitness", "Mindfulness", "Productivity", "General"
    val frequency: String = "Daily", // "Daily", "Weekly"
    val targetCount: Int = 1,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val reminderTime: String? = null, // "HH:mm" e.g., "08:00"
    val isNotificationEnabled: Boolean = false,
    val lastCompletedDate: String? = null // "yyyy-MM-dd" of last check-in to compute streaks
)

@Entity(tableName = "habit_logs")
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val date: String, // "yyyy-MM-dd"
    val isCompleted: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
