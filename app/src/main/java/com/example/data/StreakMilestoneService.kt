package com.example.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StreakMilestoneService(private val dao: StreakMilestoneDao) {

    val allMilestones: Flow<List<StreakMilestone>> = dao.getAllMilestones()

    /**
     * Restores milestones from backup.
     */
    suspend fun restoreData(milestones: List<StreakMilestone>) {
        dao.clearAllMilestones()
        dao.insertMilestones(milestones)
    }

    /**
     * Seeds initial milestones if they do not exist.
     */
    suspend fun seedMilestonesIfEmpty() {
        val initialMilestones = listOf(
            StreakMilestone(
                milestoneId = "starter_3",
                name = "Consistent Starter",
                description = "Earn a 3-day continuous streak on any habit.",
                targetStreak = 3
            ),
            StreakMilestone(
                milestoneId = "warrior_7",
                name = "7-Day Warrior",
                description = "Achieve a solid 1-week streak on any habit.",
                targetStreak = 7
            ),
            StreakMilestone(
                milestoneId = "champion_14",
                name = "Habit Champion",
                description = "Keep going for 2 weeks straight to be a Champion.",
                targetStreak = 14
            ),
            StreakMilestone(
                milestoneId = "legend_30",
                name = "Golden Legend",
                description = "Unlock the ultimate 30-day streak milestone!",
                targetStreak = 30
            )
        )
        // Only seed if the table is truly empty to preserve earned achievements
        if (dao.getMilestoneCount() == 0) {
            dao.insertMilestones(initialMilestones)
        }
    }

    /**
     * Iterates over all habits to evaluate and update milestones.
     * Calculates if the highest bestStreak among habits qualifies for new milestone unlock.
     */
    suspend fun checkAndUpdateMilestones(habits: List<Habit>) {
        if (habits.isEmpty()) return

        val maxBestStreak = habits.maxOfOrNull { it.bestStreak } ?: 0
        if (maxBestStreak <= 0) return

        val milestones = listOf(
            "starter_3" to 3,
            "warrior_7" to 7,
            "champion_14" to 14,
            "legend_30" to 30
        )
        
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        for ((id, target) in milestones) {
            if (maxBestStreak >= target) {
                val milestone = dao.getMilestoneById(id)
                if (milestone != null && !milestone.isAchieved) {
                    val updated = milestone.copy(
                        isAchieved = true,
                        achievedDate = todayStr
                    )
                    dao.updateMilestone(updated)
                }
            } else {
                // If they lost their habits or modified them, we don't revoke achievements (traditional trophy room behavior)
            }
        }
    }
}
