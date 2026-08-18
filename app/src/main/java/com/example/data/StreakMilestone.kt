package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.coroutines.flow.Flow

@IgnoreExtraProperties
@Entity(tableName = "streak_milestones")
data class StreakMilestone(
    @PrimaryKey val milestoneId: String = "", // e.g. "starter_3", "warrior_7", "champion_14", "legend_30"
    val name: String = "",
    val description: String = "",
    val targetStreak: Int = 0,
    @get:PropertyName("isAchieved") @set:PropertyName("isAchieved") var isAchieved: Boolean = false,
    val achievedDate: String? = null
)

@Dao
interface StreakMilestoneDao {
    @Query("SELECT * FROM streak_milestones ORDER BY targetStreak ASC")
    fun getAllMilestones(): Flow<List<StreakMilestone>>

    @Query("SELECT COUNT(*) FROM streak_milestones")
    suspend fun getMilestoneCount(): Int

    @Query("SELECT * FROM streak_milestones WHERE milestoneId = :id")
    suspend fun getMilestoneById(id: String): StreakMilestone?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestones(milestones: List<StreakMilestone>)

    @Update
    suspend fun updateMilestone(milestone: StreakMilestone)

    @Query("DELETE FROM streak_milestones")
    suspend fun clearAllMilestones()
}
