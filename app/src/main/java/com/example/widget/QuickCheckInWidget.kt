package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.data.Habit

/**
 * Quick Check-In Widget (2×2).
 * Compact widget with tappable habit chips. Completed habits "slide" to the
 * bottom of the list and display a celebration style, giving visual satisfaction.
 */
class QuickCheckInWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataProvider.getTodaySnapshot(context)

        provideContent {
            GlanceTheme {
                CheckInWidgetContent(snapshot)
            }
        }
    }
}

@Composable
private fun CheckInWidgetContent(snapshot: WidgetDataProvider.TodaySnapshot) {
    // Sort: uncompleted habits first, completed habits slide to bottom
    val uncompleted = snapshot.habits.filter { !snapshot.isHabitCompletedToday(it.id) }
    val completed = snapshot.habits.filter { snapshot.isHabitCompletedToday(it.id) }
    val allDone = uncompleted.isEmpty() && completed.isNotEmpty()

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.SurfaceDark)
            .cornerRadius(20.dp)
            .padding(12.dp)
    ) {
        // ── Header row ──
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(android.content.Intent(android.content.Intent.ACTION_MAIN).setClassName("com.aistudio.habittracker.uqmznx", "com.example.MainActivity").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (allDone) "🎉 All Done!" else "⚡ Check-In",
                style = TextStyle(
                    color = ColorProvider(if (allDone) WidgetColors.TealAccent else WidgetColors.TextWhite),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            // Progress counter badge
            Box(
                modifier = GlanceModifier
                    .background(if (allDone) WidgetColors.TealAccent else WidgetColors.PurpleAccent)
                    .cornerRadius(8.dp)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${snapshot.completedCount}/${snapshot.totalCount}",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.TextWhite),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // ── Habit list ──
        if (snapshot.habits.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add habits in the app",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.TextMuted),
                        fontSize = 12.sp
                    )
                )
            }
        } else if (allDone) {
            // ── All done celebration ──
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆",
                    style = TextStyle(fontSize = 28.sp)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "You crushed it today!",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.TealAccent),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "${snapshot.totalCount} habits completed ✨",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.TextMuted),
                        fontSize = 11.sp
                    )
                )
            }
        } else {
            LazyColumn {
                // Uncompleted habits at the top — ready to tap
                items(uncompleted.take(6), itemId = { it.id.toLong() }) { habit ->
                    PendingChip(habit = habit, snapshot = snapshot)
                }

                // Completed habits slide to bottom — show as done
                if (completed.isNotEmpty()) {
                    items(completed.take(4), itemId = { it.id.toLong() or (1L shl 32) }) { habit ->
                        CompletedChip(habit = habit, snapshot = snapshot)
                    }
                }
            }
        }
    }
}

/**
 * Chip for a habit that hasn't been completed yet — prominent and tappable.
 */
@Composable
private fun PendingChip(habit: Habit, snapshot: WidgetDataProvider.TodaySnapshot) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(WidgetColors.CardSurface)
            .cornerRadius(10.dp)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .clickable(
                actionRunCallback<ToggleHabitAction>(
                    actionParametersOf(
                        ToggleHabitAction.HabitIdKey to habit.id,
                        ToggleHabitAction.DateKey to snapshot.todayStr
                    )
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Empty circle — ready to check
        Box(
            modifier = GlanceModifier
                .size(20.dp)
                .background(WidgetColors.SurfaceLight)
                .cornerRadius(10.dp),
            contentAlignment = Alignment.Center
        ) {}

        Spacer(modifier = GlanceModifier.width(8.dp))

        Text(
            text = habit.name,
            style = TextStyle(
                color = ColorProvider(WidgetColors.TextWhite),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

/**
 * Chip for a completed habit — "slid" to bottom with a satisfying done state.
 * Shows a green teal accent with check mark and dimmed text.
 */
@Composable
private fun CompletedChip(habit: Habit, snapshot: WidgetDataProvider.TodaySnapshot) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(WidgetColors.CompletedChipBg)
            .cornerRadius(10.dp)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .clickable(
                actionRunCallback<ToggleHabitAction>(
                    actionParametersOf(
                        ToggleHabitAction.HabitIdKey to habit.id,
                        ToggleHabitAction.DateKey to snapshot.todayStr
                    )
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filled check circle
        Box(
            modifier = GlanceModifier
                .size(20.dp)
                .background(WidgetColors.TealAccent)
                .cornerRadius(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.TextWhite),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        Text(
            text = habit.name,
            style = TextStyle(
                color = ColorProvider(WidgetColors.TextMuted),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )

        Spacer(modifier = GlanceModifier.defaultWeight())

        Text(
            text = "Done ✨",
            style = TextStyle(
                color = ColorProvider(WidgetColors.TealAccent),
                fontSize = 10.sp
            )
        )
    }
}

/**
 * Receiver for the Quick Check-In widget.
 */
class QuickCheckInWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickCheckInWidget()
}
