package com.example.receiver

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

import android.media.RingtoneManager
import android.widget.RemoteViews
import com.example.data.AppDatabase
import com.example.data.HabitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getIntExtra("habitId", -1)
        val action = intent.action

        if (action == "ACTION_MARK_DONE" && habitId != -1) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val repository = HabitRepository(db.habitDao())
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    // Assume it's uncompleted and we toggle it to complete
                    repository.toggleHabitCompletion(habitId, dateStr)
                    
                    // Cancel this specific notification (we can just cancel all for simplicity)
                    NotificationManagerCompat.from(context).cancel(habitId)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val habitName = intent.getStringExtra("habitName") ?: "Habit"
        val habitDesc = intent.getStringExtra("habitDesc") ?: "Keep going with your daily goals!"
        val timeStr = intent.getStringExtra("timeStr")

        showNotification(context, habitId, habitName, habitDesc)
        
        // Re-schedule the alarm for the next day since setExact is a one-shot alarm
        if (timeStr != null) {
            scheduleNotification(context, habitId, habitName, habitDesc, timeStr)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(context: Context, habitId: Int, habitName: String, habitDesc: String) {
        val channelId = "habit_reminders"
        
        // Create Notification Channel on Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Habit Reminders"
            val descriptionText = "Get reminders to keep up with your daily habits"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Action when clicking the notification (opens main screen)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            habitId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action for "Mark Done"
        val markDoneIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "ACTION_MARK_DONE"
            putExtra("habitId", habitId)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            habitId,
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Custom Layout
        val remoteViews = RemoteViews(context.packageName, R.layout.custom_notification)
        remoteViews.setTextViewText(R.id.notification_title, "Goal Reminder: $habitName")
        remoteViews.setTextViewText(R.id.notification_desc, habitDesc)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setColor(android.graphics.Color.parseColor("#9D4EDD")) // Custom Color (Neon Purple)
            .setSound(defaultSoundUri) // Custom Sound setup (using default for now)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_edit, "Mark Done", markDonePendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(habitId, builder.build())
        }
    }

    companion object {
        fun scheduleNotification(context: Context, habitId: Int, habitName: String, habitDesc: String, timeStr: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("habitId", habitId)
                putExtra("habitName", habitName)
                putExtra("habitDesc", habitDesc)
                putExtra("timeStr", timeStr)
            }

            val times = timeStr.split(",")
            times.forEachIndexed { index, time ->
                val requestCode = habitId * 100 + index
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Parse time string e.g. "08:00"
                val parts = time.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull()
                    val minute = parts[1].toIntOrNull()
                    if (hour != null && minute != null) {
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = System.currentTimeMillis()
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            // If the time has already passed today, set it for tomorrow
                            if (timeInMillis <= System.currentTimeMillis()) {
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }

                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    calendar.timeInMillis,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.setExact(
                                    AlarmManager.RTC_WAKEUP,
                                    calendar.timeInMillis,
                                    pendingIntent
                                )
                            }
                        } catch (e: SecurityException) {
                            // Fallback if SCHEDULE_EXACT_ALARM is missing
                            alarmManager.set(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        }
                    }
                }
            }
        }

        fun cancelNotification(context: Context, habitId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, NotificationReceiver::class.java)
            for (index in 0..50) {
                val requestCode = habitId * 100 + index
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }
        }
    }
}
