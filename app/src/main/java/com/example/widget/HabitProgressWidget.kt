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
 * Today's Progress Widget (3×3).
 * Shows a progress summary header with a scrollable list of habits,
 * each tappable to toggle completion.
 */
class HabitProgressWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataProvider.getTodaySnapshot(context)

        provideContent {
            GlanceTheme {
                ProgressWidgetContent(snapshot)
            }
        }
    }
}

@Composable
private fun ProgressWidgetContent(snapshot: WidgetDataProvider.TodaySnapshot) {
    // Main dark container
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.SurfaceDark)
            .cornerRadius(20.dp)
            .padding(14.dp)
    ) {
        // ── Header ──
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(android.content.Intent(android.content.Intent.ACTION_MAIN).setClassName("com.aistudio.habittracker.uqmznx", "com.example.MainActivity").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎯",
                style = TextStyle(fontSize = 20.sp)
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Today's Progress",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.TextWhite),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = "${snapshot.completedCount} of ${snapshot.totalCount} done",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.TextMuted),
                        fontSize = 12.sp
                    )
                )
            }
            // Percentage badge
            Box(
                modifier = GlanceModifier
                    .background(WidgetColors.PurpleAccent)
                    .cornerRadius(12.dp)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(snapshot.progressFraction * 100).toInt()}%",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.TextWhite),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        // ── Progress bar ──
        WidgetProgressBar(fraction = snapshot.progressFraction)

        Spacer(modifier = GlanceModifier.height(10.dp))

        // ── Habit list ──
        if (snapshot.habits.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No habits yet — tap to add one!",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.TextMuted),
                        fontSize = 13.sp
                    )
                )
            }
        } else {
            LazyColumn {
                items(snapshot.habits, itemId = { it.id.toLong() }) { habit ->
                    HabitRow(habit = habit, snapshot = snapshot)
                }
            }
        }
    }
}

@Composable
private fun WidgetProgressBar(fraction: Float) {
    // Track
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(6.dp)
            .background(WidgetColors.SurfaceLight)
            .cornerRadius(3.dp)
    ) {
        // Fill
        if (fraction > 0f) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Box(
                    modifier = GlanceModifier
                        .height(6.dp)
                        .defaultWeight()
                        .background(WidgetColors.TealAccent)
                        .cornerRadius(3.dp)
                ) {}
                if (fraction < 1f) {
                    Spacer(modifier = GlanceModifier.width(((1f - fraction) * 200).toInt().dp))
                }
            }
        }
    }
}

@Composable
private fun HabitRow(habit: Habit, snapshot: WidgetDataProvider.TodaySnapshot) {
    val isCompleted = snapshot.isHabitCompletedToday(habit.id)
    val categoryEmoji = getCategoryEmoji(habit.category)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(WidgetColors.CardSurface)
            .cornerRadius(12.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
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
        // Category emoji
        Text(
            text = categoryEmoji,
            style = TextStyle(fontSize = 16.sp)
        )
        Spacer(modifier = GlanceModifier.width(8.dp))

        // Habit name
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = habit.name,
                style = TextStyle(
                    color = ColorProvider(if (isCompleted) WidgetColors.TextMuted else WidgetColors.TextWhite),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                ),
                maxLines = 1
            )
            if (habit.streak > 0) {
                Text(
                    text = "🔥 ${habit.streak} day streak",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.StreakOrange),
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Completion indicator
        Box(
            modifier = GlanceModifier
                .size(26.dp)
                .background(if (isCompleted) WidgetColors.TealAccent else WidgetColors.SurfaceLight)
                .cornerRadius(13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCompleted) "✓" else "",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.TextWhite),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
        }
    }
}

private fun getCategoryEmoji(category: String): String = when (category) {
    "Health" -> "💚"
    "Fitness" -> "💪"
    "Mindfulness" -> "🧘"
    "Productivity" -> "⚡"
    else -> "✨"
}

/**
 * Receiver that the system uses to instantiate the widget.
 */
class HabitProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitProgressWidget()
}
