package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "streak_milestones")
data class StreakMilestone(
    @PrimaryKey val milestoneId: String, // e.g. "starter_3", "warrior_7", "champion_14", "legend_30"
    val name: String,
    val description: String,
    val targetStreak: Int,
    val isAchieved: Boolean = false,
    val achievedDate: String? = null
)

@Dao
interface StreakMilestoneDao {
    @Query("SELECT * FROM streak_milestones ORDER BY targetStreak ASC")
    fun getAllMilestones(): Flow<List<StreakMilestone>>

    @Query("SELECT * FROM streak_milestones WHERE milestoneId = :id")
    suspend fun getMilestoneById(id: String): StreakMilestone?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestones(milestones: List<StreakMilestone>)

    @Update
    suspend fun updateMilestone(milestone: StreakMilestone)
}
