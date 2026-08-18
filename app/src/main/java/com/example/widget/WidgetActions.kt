package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.example.data.AppDatabase
import com.example.data.HabitRepository

/**
 * Called when the user taps a habit row in either widget.
 * Toggles completion for the tapped habit and refreshes ALL widget instances.
 */
class ToggleHabitAction : ActionCallback {

    companion object {
        val HabitIdKey = ActionParameters.Key<Int>("habit_id")
        val DateKey = ActionParameters.Key<String>("date")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[HabitIdKey] ?: return
        val date = parameters[DateKey] ?: return

        val db = AppDatabase.getDatabase(context)
        val repository = HabitRepository(db.habitDao())
        repository.toggleHabitCompletion(habitId, date)

        // updateAll() refreshes every placed instance of each widget type
        HabitProgressWidget().updateAll(context)
        QuickCheckInWidget().updateAll(context)
    }
}

