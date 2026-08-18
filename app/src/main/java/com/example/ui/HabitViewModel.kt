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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository
    private val milestoneService: StreakMilestoneService
    private val firestoreRepository: FirestoreRepository
    private val themePreferences: com.example.data.ThemePreferences
    val habits: StateFlow<List<Habit>>
    val logs: StateFlow<List<HabitLog>>
    val milestones: StateFlow<List<StreakMilestone>>

    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Leaderboard & Fair Scoring state
    val leaderboardEntries: StateFlow<List<LeaderboardEntry>>
    val scoreBreakdown: StateFlow<com.example.data.ScoreBreakdown>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HabitRepository(database.habitDao())
        milestoneService = StreakMilestoneService(database.streakMilestoneDao())
        firestoreRepository = FirestoreRepository()
        themePreferences = com.example.data.ThemePreferences(application)
        
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

        leaderboardEntries = firestoreRepository.getLeaderboard()
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        scoreBreakdown = combine(habits, logs, milestones) { hList, lList, mList ->
            com.example.data.ScoringEngine.calculateScore(hList, lList, mList)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.example.data.ScoreBreakdown()
        )

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        _selectedDate.value = sdf.format(Calendar.getInstance().time)

        // Seed initial data if empty to show beautiful charts and engage the user immediately
        viewModelScope.launch {
            try {
                val initialHabits = repository.allHabits.firstOrNull()
                if (initialHabits.isNullOrEmpty()) {
                    val backup = firestoreRepository.restoreUserData()
                    if (backup != null && backup.habits.isNotEmpty()) {
                        repository.restoreData(backup.habits, backup.logs)
                        milestoneService.restoreData(backup.milestones)
                    } else {
                        milestoneService.seedMilestonesIfEmpty()
                    }
                } else {
                    milestoneService.seedMilestonesIfEmpty()
                }
            } catch (e: Exception) {
                android.util.Log.e("HabitViewModel", "Failed to restore or seed data on launch", e)
                try {
                    milestoneService.seedMilestonesIfEmpty()
                } catch (_: Exception) {}
            } finally {
                kotlinx.coroutines.delay(600)
                _isLoading.value = false
            }

            try {
                repository.allHabits.collect { list ->
                    milestoneService.checkAndUpdateMilestones(list)
                }
            } catch (e: Exception) {
                android.util.Log.e("HabitViewModel", "Failed collecting habits for milestones", e)
            }
        }

        // Reactive automatic cloud backup whenever habits, logs, or milestones change locally
        viewModelScope.launch {
            combine(
                repository.allHabits,
                repository.allLogs,
                milestoneService.allMilestones
            ) { habitsList, logsList, milestonesList ->
                Triple(habitsList, logsList, milestonesList)
            }
            .drop(1)
            .debounce(1000)
            .collect { (h, l, m) ->
                try {
                    if (firestoreRepository.getCurrentUserId() != null && h.isNotEmpty()) {
                        firestoreRepository.backupUserData(h, l, m)
                        val totalCompletions = l.size
                        firestoreRepository.syncLeaderboard(h, l, m)
                        android.util.Log.d("HabitViewModel", "Reactive auto-backup completed successfully!")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HabitViewModel", "Reactive auto-backup failed", e)
                }
            }
        }
    }

    fun setSelectedDate(dateStr: String) {
        _selectedDate.value = dateStr
    }

    fun refreshTodayDate() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Calendar.getInstance().time)
        if (_selectedDate.value != today) {
            _selectedDate.value = today
        }
    }

    fun addHabit(
        name: String,
        description: String,
        category: String,
        frequency: String,
        targetCount: Int,
        reminderTime: String?,
        isNotificationEnabled: Boolean
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
                    getApplication<Application>(),
                    id.toInt(),
                    name,
                    description.ifEmpty { "Keep going with your habit!" },
                    reminderTime
                )
            }
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
            if (habit.isNotificationEnabled && habit.reminderTime != null) {
                NotificationReceiver.scheduleNotification(
                    getApplication<Application>(),
                    habit.id,
                    habit.name,
                    habit.description.ifEmpty { "Keep going with your habit!" },
                    habit.reminderTime
                )
            } else {
                NotificationReceiver.cancelNotification(getApplication<Application>(), habit.id)
            }
        }
    }

    fun toggleHabitCompletion(habitId: Int, dateStr: String) {
        viewModelScope.launch {
            repository.toggleHabitCompletion(habitId, dateStr)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            NotificationReceiver.cancelNotification(getApplication<Application>(), habit.id)
            repository.deleteHabit(habit)
        }
    }

    fun checkAndRestoreFromCloud() {
        viewModelScope.launch {
            try {
                if (firestoreRepository.getCurrentUserId() != null) {
                    val currentHabits = repository.allHabits.firstOrNull()
                    if (currentHabits.isNullOrEmpty()) {
                        val backup = firestoreRepository.restoreUserData()
                        if (backup != null && backup.habits.isNotEmpty()) {
                            repository.restoreData(backup.habits, backup.logs)
                            milestoneService.restoreData(backup.milestones)
                            android.util.Log.d("HabitViewModel", "Successfully auto-restored cloud backup for user!")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HabitViewModel", "Auto cloud restore failed", e)
            }
        }
    }
}

