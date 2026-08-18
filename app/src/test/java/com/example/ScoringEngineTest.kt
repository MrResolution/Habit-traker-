package com.example

import com.example.data.Habit
import com.example.data.HabitLog
import com.example.data.ScoringEngine
import com.example.data.StreakMilestone
import org.junit.Assert.*
import org.junit.Test

class ScoringEngineTest {

    @Test
    fun `calculateScore returns default breakdown for empty habits`() {
        val breakdown = ScoringEngine.calculateScore(emptyList(), emptyList())
        assertEquals(0, breakdown.totalScore)
        assertEquals(1, breakdown.levelInfo.level)
        assertEquals("Novice", breakdown.levelInfo.title)
    }

    @Test
    fun `calculateScore correctly weights daily vs weekly habits`() {
        val dailyHabit = Habit(id = 1, name = "Water", frequency = "Daily", targetCount = 1, streak = 5, bestStreak = 5)
        val weeklyHabit = Habit(id = 2, name = "Gym", frequency = "Weekly", targetCount = 1, streak = 2, bestStreak = 2)
        val habits = listOf(dailyHabit, weeklyHabit)

        val logs = listOf(
            HabitLog(id = 101, habitId = 1, date = "2026-08-15"),
            HabitLog(id = 102, habitId = 2, date = "2026-08-15")
        )

        val breakdown = ScoringEngine.calculateScore(habits, logs, todayStr = "2026-08-15")

        // Daily completion = 10 pts, Weekly completion = 30 pts => Base = 40 pts
        assertEquals(40, breakdown.basePoints)

        // Primary streak = 5, active streak sum = 7, best streak sum = 7
        // Streak synergy = (5 * 15) + (7 * 5) + (7 * 3) = 75 + 35 + 21 = 131 pts
        assertEquals(131, breakdown.streakSynergyPoints)

        assertTrue("Total score should be greater than zero", breakdown.totalScore > 0)
    }

    @Test
    fun `calculateScore applies multi-habit streak synergy fairly`() {
        // User A: 1 habit with 30-day streak
        val userAHabits = listOf(Habit(id = 1, name = "Meditation", streak = 30, bestStreak = 30))
        val userALogs = (0 until 30).map { i -> HabitLog(id = i, habitId = 1, date = "2026-08-${15 - i}") }
        val userABreakdown = ScoringEngine.calculateScore(userAHabits, userALogs, todayStr = "2026-08-15")

        // User B: 3 habits with 15-day streak each
        val userBHabits = listOf(
            Habit(id = 10, name = "Read", streak = 15, bestStreak = 15),
            Habit(id = 11, name = "Exercise", streak = 15, bestStreak = 15),
            Habit(id = 12, name = "Code", streak = 15, bestStreak = 15)
        )
        val userBLogs = mutableListOf<HabitLog>()
        for (h in userBHabits) {
            for (i in 0 until 15) {
                userBLogs.add(HabitLog(id = h.id * 100 + i, habitId = h.id, date = "2026-08-${15 - i}"))
            }
        }
        val userBBreakdown = ScoringEngine.calculateScore(userBHabits, userBLogs, todayStr = "2026-08-15")

        // User B maintains 3 active habits consistently and gets rewarded for multi-habit streak synergy
        assertTrue("User B with multiple consistent habits should earn high streak synergy", userBBreakdown.streakSynergyPoints > 200)
    }

    @Test
    fun `calculateLevelInfo maps scores to appropriate levels`() {
        val level1 = ScoringEngine.calculateLevelInfo(0)
        assertEquals(1, level1.level)
        assertEquals("Novice", level1.title)

        val level2 = ScoringEngine.calculateLevelInfo(120)
        assertEquals(2, level2.level)
        assertEquals("Bronze Builder", level2.title)

        val level5 = ScoringEngine.calculateLevelInfo(900)
        assertEquals(5, level5.level)
        assertEquals("Silver Champion", level5.title)
    }
}
