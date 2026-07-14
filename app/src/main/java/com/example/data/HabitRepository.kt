package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HabitRepository(private val habitDao: HabitDao) {

    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allLogs: Flow<List<HabitLog>> = habitDao.getAllLogs()

    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>> = habitDao.getLogsForHabit(habitId)

    suspend fun insertHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteLogsForHabit(habit.id)
        habitDao.deleteHabit(habit)
    }

    suspend fun toggleHabitCompletion(habitId: Int, dateStr: String): Boolean {
        // Fetch habit
        val habit = habitDao.getHabitByIdSuspend(habitId) ?: return false

        // Fetch logs for this habit
        val existingLogs = habitDao.getLogsForHabit(habitId).firstOrNull() ?: emptyList()
        val isAlreadyCompleted = existingLogs.any { it.date == dateStr }

        if (isAlreadyCompleted) {
            habitDao.deleteLog(habitId, dateStr)
        } else {
            habitDao.insertLog(HabitLog(habitId = habitId, date = dateStr))
        }

        // Recalculate streak and best streak
        val updatedLogs = habitDao.getLogsForHabit(habitId).firstOrNull() ?: emptyList()
        val (currentStreak, bestStreak) = calculateStreaks(updatedLogs, dateStr)

        val updatedHabit = habit.copy(
            streak = currentStreak,
            bestStreak = maxOf(habit.bestStreak, bestStreak),
            lastCompletedDate = if (!isAlreadyCompleted) dateStr else {
                // Find latest completion date in remaining logs
                updatedLogs.firstOrNull()?.date
            }
        )
        habitDao.updateHabit(updatedHabit)
        return !isAlreadyCompleted
    }

    /**
     * Calculates the current streak and longest (best) streak based on list of logs.
     * Logs are expected to be ordered by date desc.
     */
    private fun calculateStreaks(logs: List<HabitLog>, todayStr: String): Pair<Int, Int> {
        val completedDates = logs.map { it.date }.distinct().sortedDescending()
        if (completedDates.isEmpty()) return Pair(0, 0)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = try { sdf.parse(todayStr) } catch (e: Exception) { null } ?: return Pair(0, 0)

        // Calculate current streak
        var currentStreak = 0
        val cal = Calendar.getInstance()
        cal.time = today

        val todayFormatted = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayFormatted = sdf.format(cal.time)

        val hasCompletedToday = completedDates.contains(todayFormatted)
        val hasCompletedYesterday = completedDates.contains(yesterdayFormatted)

        if (hasCompletedToday || hasCompletedYesterday) {
            var checkCal = Calendar.getInstance()
            if (hasCompletedToday) {
                checkCal.time = today
            } else {
                checkCal.time = today
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }

            while (true) {
                val checkStr = sdf.format(checkCal.time)
                if (completedDates.contains(checkStr)) {
                    currentStreak++
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }

        // Calculate best streak (longest contiguous sequence in completedDates)
        // completedDates is sorted descending (e.g. ["2026-07-13", "2026-07-12", "2026-07-10"])
        var bestStreak = 0
        var tempStreak = 0
        var prevDate: Calendar? = null

        // Process from oldest to newest to find maximum continuous streak
        val chronologicalDates = completedDates.sorted()
        for (dateStr in chronologicalDates) {
            val currDate = Calendar.getInstance()
            currDate.time = sdf.parse(dateStr)!!

            if (prevDate == null) {
                tempStreak = 1
            } else {
                val diffDays = getDaysDifference(prevDate, currDate)
                if (diffDays == 1) {
                    tempStreak++
                } else if (diffDays > 1) {
                    bestStreak = maxOf(bestStreak, tempStreak)
                    tempStreak = 1
                }
            }
            prevDate = currDate
        }
        bestStreak = maxOf(bestStreak, tempStreak)

        return Pair(currentStreak, maxOf(bestStreak, currentStreak))
    }

    private fun getDaysDifference(cal1: Calendar, cal2: Calendar): Int {
        val ms1 = cal1.timeInMillis
        val ms2 = cal2.timeInMillis
        val diffMs = ms2 - ms1
        // Avoid daylight saving hour shifts by adding margin and rounding
        return ((diffMs + 1000 * 60 * 60 * 2) / (1000 * 60 * 60 * 24)).toInt()
    }
}
