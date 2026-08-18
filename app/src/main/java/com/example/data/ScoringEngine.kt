package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.floor
import kotlin.math.sqrt

data class ScoreBreakdown(
    val basePoints: Int = 0,
    val streakSynergyPoints: Int = 0,
    val consistencyMultiplier: Double = 1.0,
    val perfectDayBonus: Int = 0,
    val milestoneBonus: Int = 0,
    val totalScore: Int = 0,
    val levelInfo: UserLevelInfo = UserLevelInfo()
)

data class UserLevelInfo(
    val level: Int = 1,
    val title: String = "Novice",
    val currentLevelXp: Int = 0,
    val nextLevelXp: Int = 100,
    val progressPercent: Float = 0.0f
)

object ScoringEngine {

    fun calculateScore(
        habits: List<Habit>,
        logs: List<HabitLog>,
        milestones: List<StreakMilestone> = emptyList(),
        todayStr: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    ): ScoreBreakdown {
        if (habits.isEmpty()) return ScoreBreakdown()

        // 1. Base Points: Daily = 10 * targetCount, Weekly = 30 * targetCount per completion
        var basePoints = 0
        val habitMap = habits.associateBy { it.id }
        for (log in logs) {
            val h = habitMap[log.habitId] ?: continue
            val pointsPerCompletion = if (h.frequency.equals("Weekly", ignoreCase = true)) 30 else 10
            basePoints += pointsPerCompletion * h.targetCount.coerceAtLeast(1)
        }

        // 2. Multi-Habit Streak Synergy Points
        val currentStreaks = habits.map { it.streak }
        val bestStreaks = habits.map { it.bestStreak }

        val primaryStreak = if (currentStreaks.isNotEmpty()) currentStreaks.maxOrNull() ?: 0 else 0
        val activeHabitStreakSum = currentStreaks.sum()
        val bestStreakSum = bestStreaks.sum()

        // Multi-habit streak formula rewards both peak streak and total multi-habit streak consistency
        val streakSynergyPoints = (primaryStreak * 15) + (activeHabitStreakSum * 5) + (bestStreakSum * 3)

        // 3. 30-Day Rolling Consistency Multiplier (1.0x to 1.5x)
        val consistencyMultiplier = calculate30DayConsistencyMultiplier(habits, logs, todayStr)

        // 4. Perfect Day Bonus (+25 pts per perfect day in last 30 days)
        val perfectDayBonus = calculatePerfectDayBonus(habits, logs, todayStr)

        // 5. Unlocked Milestone Bonus
        val milestoneBonus = milestones.filter { it.isAchieved }.sumOf { milestone ->
            when (milestone.targetStreak) {
                3 -> 50
                7 -> 100
                14 -> 250
                30 -> 500
                else -> 50
            }
        }

        // Total Score formula
        val subtotal = (basePoints + streakSynergyPoints + perfectDayBonus + milestoneBonus)
        val finalScore = (subtotal * consistencyMultiplier).toInt()

        val levelInfo = calculateLevelInfo(finalScore)

        return ScoreBreakdown(
            basePoints = basePoints,
            streakSynergyPoints = streakSynergyPoints,
            consistencyMultiplier = consistencyMultiplier,
            perfectDayBonus = perfectDayBonus,
            milestoneBonus = milestoneBonus,
            totalScore = finalScore,
            levelInfo = levelInfo
        )
    }

    private fun calculate30DayConsistencyMultiplier(
        habits: List<Habit>,
        logs: List<HabitLog>,
        todayStr: String
    ): Double {
        if (habits.isEmpty()) return 1.0

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = try { sdf.parse(todayStr) } catch (e: Exception) { null } ?: return 1.0
        val cal = Calendar.getInstance()

        val datesIn30Days = mutableListOf<String>()
        for (i in 0 until 30) {
            cal.time = today
            cal.add(Calendar.DAY_OF_YEAR, -i)
            datesIn30Days.add(sdf.format(cal.time))
        }

        val logsIn30Days = logs.filter { it.date in datesIn30Days }
        val actualCompletions = logsIn30Days.size

        // Expected completions in 30 days based on active habits
        var expectedCompletions = 0
        for (h in habits) {
            val habitAgeDays = ((System.currentTimeMillis() - h.createdTimestamp) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
            if (h.frequency.equals("Weekly", ignoreCase = true)) {
                val expectedWeeks = (habitAgeDays / 7).coerceIn(1, 4)
                expectedCompletions += expectedWeeks * h.targetCount.coerceAtLeast(1)
            } else {
                val cappedDays = habitAgeDays.coerceAtMost(30)
                expectedCompletions += cappedDays * h.targetCount.coerceAtLeast(1)
            }
        }

        if (expectedCompletions <= 0) return 1.0

        val ratio = (actualCompletions.toDouble() / expectedCompletions.toDouble()).coerceIn(0.0, 1.0)
        // Multiplier scales smoothly from 1.0x to 1.5x
        return 1.0 + (ratio * 0.5)
    }

    private fun calculatePerfectDayBonus(
        habits: List<Habit>,
        logs: List<HabitLog>,
        todayStr: String
    ): Int {
        val dailyHabits = habits.filter { !it.frequency.equals("Weekly", ignoreCase = true) }
        if (dailyHabits.isEmpty()) return 0

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = try { sdf.parse(todayStr) } catch (e: Exception) { null } ?: return 0
        val cal = Calendar.getInstance()

        var perfectDays = 0
        val dailyHabitIds = dailyHabits.map { it.id }.toSet()

        // Check last 30 days
        for (i in 0 until 30) {
            cal.time = today
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dStr = sdf.format(cal.time)

            val completedHabitIdsOnDate = logs.filter { it.date == dStr && it.habitId in dailyHabitIds }.map { it.habitId }.toSet()
            if (completedHabitIdsOnDate.size == dailyHabitIds.size) {
                perfectDays++
            }
        }
        return perfectDays * 25
    }

    fun calculateLevelInfo(totalScore: Int): UserLevelInfo {
        if (totalScore <= 0) return UserLevelInfo(1, "Novice", 0, 100, 0f)

        val rawLevel = floor(sqrt(totalScore.toDouble() / 50.0)).toInt() + 1
        val level = rawLevel.coerceIn(1, 15)

        val levelTitles = mapOf(
            1 to "Novice",
            2 to "Bronze Builder",
            3 to "Bronze Master",
            4 to "Silver Tracker",
            5 to "Silver Champion",
            6 to "Gold Achiever",
            7 to "Gold Dominator",
            8 to "Platinum Specialist",
            9 to "Platinum Elite",
            10 to "Diamond Legend",
            11 to "Diamond Legend II",
            12 to "Mythic Tracker",
            13 to "Mythic Overlord",
            14 to "Supreme Legend",
            15 to "Immortal Titan"
        )
        val title = levelTitles[level] ?: "Immortal Titan"

        // XP thresholds
        val currentLevelMinXp = ((level - 1) * (level - 1) * 50)
        val nextLevelMinXp = (level * level * 50)
        val range = (nextLevelMinXp - currentLevelMinXp).coerceAtLeast(1)

        val xpInCurrentLevel = (totalScore - currentLevelMinXp).coerceAtLeast(0)
        val progressPercent = (xpInCurrentLevel.toFloat() / range.toFloat()).coerceIn(0f, 1f)

        return UserLevelInfo(
            level = level,
            title = title,
            currentLevelXp = xpInCurrentLevel,
            nextLevelXp = range,
            progressPercent = progressPercent
        )
    }
}
