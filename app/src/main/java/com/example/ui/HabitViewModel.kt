package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Habit
import com.example.data.HabitLog
import com.example.data.HabitRepository
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
    val habits: StateFlow<List<Habit>>
    val logs: StateFlow<List<HabitLog>>
    val milestones: StateFlow<List<StreakMilestone>>

    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HabitRepository(database.habitDao())
        milestoneService = StreakMilestoneService(database.streakMilestoneDao())
        
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

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        _selectedDate.value = sdf.format(Calendar.getInstance().time)

        // Seed initial data if empty to show beautiful charts and engage the user immediately
        viewModelScope.launch {
            milestoneService.seedMilestonesIfEmpty()
            repository.allHabits.collect { list ->
                if (list.isEmpty()) {
                    seedInitialData()
                } else {
                    milestoneService.checkAndUpdateMilestones(list)
                }
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
        }
    }

    fun deleteHabit(habit: Habit, context: Context) {
        viewModelScope.launch {
            NotificationReceiver.cancelNotification(context, habit.id)
            repository.deleteHabit(habit)
        }
    }

    private suspend fun seedInitialData() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        val today = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val twoDaysAgo = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val threeDaysAgo = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val fourDaysAgo = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val fiveDaysAgo = sdf.format(cal.time)

        // Habit 1: Hydration
        val h1Id = repository.insertHabit(
            Habit(
                name = "Drink 3L Water",
                description = "Stay energized and clear-minded",
                category = "Health",
                frequency = "Daily",
                streak = 3,
                bestStreak = 4,
                reminderTime = "08:00",
                isNotificationEnabled = false,
                lastCompletedDate = today
            )
        )
        repository.toggleHabitCompletion(h1Id.toInt(), fiveDaysAgo)
        repository.toggleHabitCompletion(h1Id.toInt(), threeDaysAgo)
        repository.toggleHabitCompletion(h1Id.toInt(), twoDaysAgo)
        repository.toggleHabitCompletion(h1Id.toInt(), yesterday)
        repository.toggleHabitCompletion(h1Id.toInt(), today)

        // Habit 2: Mindfulness
        val h2Id = repository.insertHabit(
            Habit(
                name = "Morning Zen",
                description = "10 minutes of deep breathing and gratitude",
                category = "Mindfulness",
                frequency = "Daily",
                streak = 2,
                bestStreak = 3,
                reminderTime = "07:30",
                isNotificationEnabled = false,
                lastCompletedDate = today
            )
        )
        repository.toggleHabitCompletion(h2Id.toInt(), fourDaysAgo)
        repository.toggleHabitCompletion(h2Id.toInt(), threeDaysAgo)
        repository.toggleHabitCompletion(h2Id.toInt(), yesterday)
        repository.toggleHabitCompletion(h2Id.toInt(), today)

        // Habit 3: Coding
        val h3Id = repository.insertHabit(
            Habit(
                name = "Code 1 Hour",
                description = "Build personal projects and learn new libraries",
                category = "Productivity",
                frequency = "Daily",
                streak = 1,
                bestStreak = 2,
                reminderTime = "20:00",
                isNotificationEnabled = false,
                lastCompletedDate = today
            )
        )
        repository.toggleHabitCompletion(h3Id.toInt(), fiveDaysAgo)
        repository.toggleHabitCompletion(h3Id.toInt(), fourDaysAgo)
        repository.toggleHabitCompletion(h3Id.toInt(), today)
    }
}
