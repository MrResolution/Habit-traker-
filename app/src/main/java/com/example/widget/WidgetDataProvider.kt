package com.example.widget

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.Habit
import com.example.data.HabitLog
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Lightweight data layer for widgets.
 * Reads directly from Room to avoid depending on the ViewModel lifecycle.
 */
object WidgetDataProvider {

    data class TodaySnapshot(
        val habits: List<Habit>,
        val todayLogs: List<HabitLog>,
        val todayStr: String
    ) {
        val completedCount: Int get() = todayLogs.distinctBy { it.habitId }.size
        val totalCount: Int get() = habits.size
        val progressFraction: Float
            get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount

        fun isHabitCompletedToday(habitId: Int): Boolean =
            todayLogs.any { it.habitId == habitId }
    }

    suspend fun getTodaySnapshot(context: Context): TodaySnapshot {
        val db = AppDatabase.getDatabase(context)
        val dao = db.habitDao()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Calendar.getInstance().time)

        val habits = dao.getAllHabits().firstOrNull() ?: emptyList()
        val todayLogs = dao.getLogsForDate(todayStr).firstOrNull() ?: emptyList()

        return TodaySnapshot(habits = habits, todayLogs = todayLogs, todayStr = todayStr)
    }
}
