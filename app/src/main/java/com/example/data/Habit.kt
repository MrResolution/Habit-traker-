package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val category: String = "General", // "Health", "Fitness", "Mindfulness", "Productivity", "General"
    val frequency: String = "Daily", // "Daily", "Weekly"
    val targetCount: Int = 1,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val reminderTime: String? = null, // "HH:mm" e.g., "08:00"
    @get:PropertyName("isNotificationEnabled") @set:PropertyName("isNotificationEnabled") var isNotificationEnabled: Boolean = false,
    val lastCompletedDate: String? = null // "yyyy-MM-dd" of last check-in to compute streaks
)

@IgnoreExtraProperties
@Entity(
    tableName = "habit_logs",
    indices = [Index(value = ["habitId", "date"], unique = true)]
)
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int = 0,
    val date: String = "", // "yyyy-MM-dd"
    @get:PropertyName("isCompleted") @set:PropertyName("isCompleted") var isCompleted: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
