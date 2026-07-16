package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.FirestoreRepository
import com.example.data.Habit
import com.example.data.HabitLog
import com.example.data.HabitRepository
import com.example.data.LeaderboardEntry
import com.example.data.StreakMilestone
import com.example.data.StreakMilestoneService
import com.example.receiver.NotificationReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository
    private val milestoneService: StreakMilestoneService
    private val firestoreRepository: FirestoreRepository
    val habits: StateFlow<List<Habit>>
    val logs: StateFlow<List<HabitLog>>
    val milestones: StateFlow<List<StreakMilestone>>

    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Leaderboard state from Firestore
    val leaderboardEntries: StateFlow<List<LeaderboardEntry>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HabitRepository(database.habitDao())
        milestoneService = StreakMilestoneService(database.streakMilestoneDao())
        firestoreRepository = FirestoreRepository()
        
        habits = repository.allHabits.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        logs = repository.allLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        milestones = milestoneService.allMilestones.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        leaderboardEntries = firestoreRepository.getLeaderboard().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        _selectedDate.value = sdf.format(Calendar.getInstance().time)

        // Seed initial data if empty to show beautiful charts and engage the user immediately
        viewModelScope.launch {
            milestoneService.seedMilestonesIfEmpty()
            repository.allHabits.collect { list ->
                milestoneService.checkAndUpdateMilestones(list)
            }
        }
    }

    fun setSelectedDate(dateStr: String) {
        _selectedDate.value = dateStr
    }

    fun addHabit(
        name: String,
        description: String,
        category: String,
        frequency: String,
        targetCount: Int,
        reminderTime: String?,
        isNotificationEnabled: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            val habit = Habit(
                name = name,
                description = description,
                category = category,
                frequency = frequency,
                targetCount = targetCount,
                reminderTime = reminderTime,
                isNotificationEnabled = isNotificationEnabled
            )
            val id = repository.insertHabit(habit)
            if (isNotificationEnabled && reminderTime != null) {
                NotificationReceiver.scheduleNotification(
                    context,
                    id.toInt(),
                    name,
                    description.ifEmpty { "Keep going with your habit!" },
                    reminderTime
                )
            }
            // Sync to leaderboard after adding habit
            syncToLeaderboard()
        }
    }

    fun updateHabit(habit: Habit, context: Context) {
        viewModelScope.launch {
            repository.updateHabit(habit)
            if (habit.isNotificationEnabled && habit.reminderTime != null) {
                NotificationReceiver.scheduleNotification(
                    context,
                    habit.id,
                    habit.name,
                    habit.description.ifEmpty { "Keep going with your habit!" },
                    habit.reminderTime
                )
            } else {
                NotificationReceiver.cancelNotification(context, habit.id)
            }
        }
    }

    fun toggleHabitCompletion(habitId: Int, dateStr: String) {
        viewModelScope.launch {
            repository.toggleHabitCompletion(habitId, dateStr)
            // Sync to leaderboard after completion toggle
            syncToLeaderboard()
        }
    }

    fun deleteHabit(habit: Habit, context: Context) {
        viewModelScope.launch {
            NotificationReceiver.cancelNotification(context, habit.id)
            repository.deleteHabit(habit)
            // Sync to leaderboard after deletion
            syncToLeaderboard()
        }
    }

    /**
     * Syncs the current user's stats to the Firestore leaderboard.
     * Called after habit completions, additions, or deletions.
     */
    private suspend fun syncToLeaderboard() {
        try {
            val currentHabits = habits.value
            val currentLogs = logs.value
            val currentMilestones = milestones.value
            val totalCompletions = currentLogs.size
            firestoreRepository.syncLeaderboard(currentHabits, totalCompletions)
            firestoreRepository.backupUserData(currentHabits, currentLogs, currentMilestones)
        } catch (e: Exception) {
            // Non-critical: leaderboard sync failure shouldn't crash the app
            android.util.Log.e("HabitViewModel", "Leaderboard sync failed", e)
        }
    }

    /**
     * Manually backs up data to the cloud.
     */
    fun forceBackup() {
        viewModelScope.launch {
            val currentHabits = habits.value
            val currentLogs = logs.value
            val currentMilestones = milestones.value
            firestoreRepository.backupUserData(currentHabits, currentLogs, currentMilestones)
        }
    }

    /**
     * Restores user state from the cloud backup and overwrites local data.
     */
    fun restoreFromCloud(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val backup = firestoreRepository.restoreUserData()
            if (backup != null) {
                repository.restoreData(backup.habits, backup.logs)
                milestoneService.restoreData(backup.milestones)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }
}
