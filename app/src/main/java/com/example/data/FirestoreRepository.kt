package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore data classes matching the cloud schema.
 * These are used for syncing local Room data to Firestore for leaderboard functionality.
 */
@IgnoreExtraProperties
data class FirestoreUser(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val totalHabits: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val joinedAt: Long = 0L
)

@IgnoreExtraProperties
data class LeaderboardEntry(
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val totalCompletions: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val score: Int = 0,
    val level: Int = 1,
    val levelTitle: String = "Novice",
    val basePoints: Int = 0,
    val streakSynergyPoints: Int = 0,
    val consistencyMultiplier: Double = 1.0,
    val perfectDayBonus: Int = 0,
    val milestoneBonus: Int = 0,
    val lastUpdated: Long = 0L
)

@IgnoreExtraProperties
data class CloudBackup(
    val habits: List<Habit> = emptyList(),
    val logs: List<HabitLog> = emptyList(),
    val milestones: List<StreakMilestone> = emptyList(),
    val lastUpdated: Long = 0L
)

/**
 * Repository that handles Firestore operations for habits, leaderboard,
 * and user profile syncing.
 */
class FirestoreRepository {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    companion object {
        private const val TAG = "FirestoreRepository"
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // ─── Leaderboard ────────────────────────────────────────────────────

    /**
     * Returns a real-time Flow of leaderboard entries sorted by score descending.
     */
    fun getLeaderboard(): Flow<List<LeaderboardEntry>> = callbackFlow {
        val listener = db.collection("leaderboard")
            .orderBy("score", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Leaderboard listen failed", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val entries = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<LeaderboardEntry>()
                } ?: emptyList()
                trySend(entries)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Update the current user's leaderboard entry based on local habit data.
     * Called after toggling completions so the leaderboard stays up-to-date.
     */
    suspend fun syncLeaderboard(
        habits: List<Habit>,
        logs: List<HabitLog> = emptyList(),
        milestones: List<StreakMilestone> = emptyList()
    ) {
        val uid = getCurrentUserId() ?: return
        try {
            val userDoc = db.collection("users").document(uid).get().await()
            if (!userDoc.exists()) {
                Log.w(TAG, "User document not found for uid: $uid, skipping leaderboard sync")
                return
            }
            val displayName = userDoc.getString("displayName") ?: "Anonymous"
            val photoUrl = userDoc.getString("photoUrl") ?: ""

            val currentStreak = if (habits.isNotEmpty()) habits.maxOf { it.streak } else 0
            val bestStreak = if (habits.isNotEmpty()) habits.maxOf { it.bestStreak } else 0
            val totalCompletions = logs.size

            // Use ScoringEngine for multi-dimensional fair scoring calculation
            val breakdown = ScoringEngine.calculateScore(habits, logs, milestones)
            val score = breakdown.totalScore

            val leaderboardData = hashMapOf(
                "userId" to uid,
                "displayName" to displayName,
                "photoUrl" to photoUrl,
                "totalCompletions" to totalCompletions,
                "currentStreak" to currentStreak,
                "bestStreak" to bestStreak,
                "score" to score,
                "level" to breakdown.levelInfo.level,
                "levelTitle" to breakdown.levelInfo.title,
                "basePoints" to breakdown.basePoints,
                "streakSynergyPoints" to breakdown.streakSynergyPoints,
                "consistencyMultiplier" to breakdown.consistencyMultiplier,
                "perfectDayBonus" to breakdown.perfectDayBonus,
                "milestoneBonus" to breakdown.milestoneBonus,
                "lastUpdated" to System.currentTimeMillis()
            )
            db.collection("leaderboard").document(uid).set(leaderboardData).await()

            // Also update user profile stats
            val userUpdates = hashMapOf<String, Any>(
                "totalHabits" to habits.size,
                "currentStreak" to currentStreak,
                "bestStreak" to bestStreak,
                "totalCompletions" to totalCompletions,
                "score" to score,
                "level" to breakdown.levelInfo.level,
                "levelTitle" to breakdown.levelInfo.title
            )
            db.collection("users").document(uid).update(userUpdates).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync leaderboard", e)
        }
    }

    /**
     * Update the user's display name across user doc and leaderboard.
     */
    suspend fun updateDisplayName(newName: String) {
        val uid = getCurrentUserId() ?: return
        try {
            db.collection("users").document(uid)
                .update("displayName", newName).await()
            db.collection("leaderboard").document(uid)
                .update("displayName", newName).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update display name", e)
        }
    }

    /**
     * Backups all user state (habits, logs, milestones) to the cloud automatically.
     */
    suspend fun backupUserData(habits: List<Habit>, logs: List<HabitLog>, milestones: List<StreakMilestone>): Boolean {
        val uid = getCurrentUserId() ?: return false
        return try {
            val backup = CloudBackup(habits, logs, milestones, System.currentTimeMillis())
            db.collection("users").document(uid).collection("data").document("backup").set(backup).await()
            Log.d(TAG, "Successfully backed up user data")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup user data", e)
            false
        }
    }

    /**
     * Restores all user state from the cloud automatically upon first launch/login.
     */
    suspend fun restoreUserData(): CloudBackup? {
        val uid = getCurrentUserId() ?: return null
        return try {
            val doc = db.collection("users").document(uid).collection("data").document("backup").get().await()
            if (doc.exists()) {
                doc.toObject<CloudBackup>()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore user data", e)
            null
        }
    }
}
