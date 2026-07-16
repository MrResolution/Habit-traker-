package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.Habit
import com.example.data.HabitLog
import com.example.data.StreakMilestone
import com.example.ui.HabitViewModel
import com.example.data.LeaderboardEntry
import com.example.data.ThemeMode
import com.example.data.ThemePreferences
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.MonthlyHeatmapGrid
import com.example.ui.components.WeeklyTrendChart
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StreakRose
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    userName: String = "User",
    themePreferences: ThemePreferences
) {
    val context = LocalContext.current
    val habits by viewModel.habits.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val milestones by viewModel.milestones.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val leaderboardEntries by viewModel.leaderboardEntries.collectAsState()

    var activeTab by remember { mutableStateOf("today") } // "today", "charts", "leaderboard", "manage"
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }

    // Check & Ask Notification Permission
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val avatarIndex by themePreferences.selectedAvatarIndex.collectAsState()
                        val avatarRes = when (avatarIndex) {
                            1 -> com.example.R.drawable.av1
                            2 -> com.example.R.drawable.av2
                            3 -> com.example.R.drawable.av3
                            4 -> com.example.R.drawable.av4
                            5 -> com.example.R.drawable.av5
                            6 -> com.example.R.drawable.av6
                            7 -> com.example.R.drawable.av7
                            8 -> com.example.R.drawable.av8
                            9 -> com.example.R.drawable.av9
                            else -> com.example.R.drawable.av1
                        }

                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = avatarRes),
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .clickable { showProfileSheet = true },
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = "Hi, $userName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = StreakRose,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val totalStreak = habits.sumOf { it.streak }
                                Text(
                                    text = "$totalStreak Day Streak",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (activeTab == "today") {
                        IconButton(
                            onClick = { showAddHabitDialog = true },
                            modifier = Modifier.testTag("add_habit_top_bar_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Habit",
                                tint = NeonPurple
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == "today",
                    onClick = { activeTab = "today" },
                    icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                    label = { Text("Today") },
                    modifier = Modifier.testTag("tab_today")
                )
                NavigationBarItem(
                    selected = activeTab == "charts",
                    onClick = { activeTab = "charts" },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Charts") },
                    label = { Text("Insights") },
                    modifier = Modifier.testTag("tab_charts")
                )
                NavigationBarItem(
                    selected = activeTab == "leaderboard",
                    onClick = { activeTab = "leaderboard" },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Leaderboard") },
                    label = { Text("Board") },
                    modifier = Modifier.testTag("tab_leaderboard")
                )
                NavigationBarItem(
                    selected = activeTab == "manage",
                    onClick = { activeTab = "manage" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Manage") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("tab_manage")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                "today" -> TodayTab(
                    habits = habits,
                    logs = logs,
                    selectedDate = selectedDate,
                    hasPermission = hasNotificationPermission,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onToggle = { habitId -> viewModel.toggleHabitCompletion(habitId, selectedDate) },
                    onDelete = { habit -> viewModel.deleteHabit(habit, context) },
                    onAddClick = { showAddHabitDialog = true }
                )
                "charts" -> ChartsTab(
                    habits = habits,
                    logs = logs,
                    milestones = milestones
                )
                "leaderboard" -> LeaderboardTab(
                    leaderboardEntries = leaderboardEntries
                )
                "manage" -> ManageTab(
                    habits = habits,
                    onToggleNotification = { habit, enabled ->
                        viewModel.updateHabit(habit.copy(isNotificationEnabled = enabled), context)
                    },
                    onDelete = { habit -> viewModel.deleteHabit(habit, context) },
                    themePreferences = themePreferences
                )
            }

            if (showAddHabitDialog) {
                AddHabitDialog(
                    onDismiss = { showAddHabitDialog = false },
                    onConfirm = { name, desc, category, freq, target, reminder, notifEnabled ->
                        viewModel.addHabit(name, desc, category, freq, target, reminder, notifEnabled, context)
                        showAddHabitDialog = false
                    }
                )
            }

            if (showProfileSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showProfileSheet = false }
                ) {
                    ProfileMenuSheetContent(
                        themePreferences = themePreferences,
                        onLogout = {
                            showProfileSheet = false
                            onLogout()
                        },
                        onDismiss = { showProfileSheet = false }
                    )
                }
            }
        }
    }
}

@Composable
fun TodayTab(
    habits: List<Habit>,
    logs: List<HabitLog>,
    selectedDate: String,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onToggle: (Int) -> Unit,
    onDelete: (Habit) -> Unit,
    onAddClick: () -> Unit
) {
    val logsToday = logs.filter { it.date == selectedDate }
    val completedIds = logsToday.map { it.habitId }.toSet()

    val totalHabits = habits.size
    val completedCount = habits.count { completedIds.contains(it.id) }
    val completionPercent = if (totalHabits > 0) (completedCount.toFloat() / totalHabits) else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Header Panel (Custom-drawn vector-like look)
        item {
            HeaderSection(
                completionPercent = completionPercent,
                completedCount = completedCount,
                totalHabits = totalHabits
            )
        }

        // Notification Permission Prompt Card
        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                PermissionPromptCard(onRequestPermission = onRequestPermission)
            }
        }

        // Habits List Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Goals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = selectedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // List of Habits
        if (habits.isEmpty()) {
            item {
                EmptyStateCard(onAddClick = onAddClick)
            }
        } else {
            items(habits, key = { it.id }) { habit ->
                val isCompleted = completedIds.contains(habit.id)
                HabitItemCard(
                    habit = habit,
                    isCompleted = isCompleted,
                    onToggle = { onToggle(habit.id) },
                    onDelete = { onDelete(habit) }
                )
            }
        }
    }
}

@Composable
fun HeaderSection(
    completionPercent: Float,
    completedCount: Int,
    totalHabits: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        NeonPurple.copy(alpha = 0.25f),
                        CyberTeal.copy(alpha = 0.1f)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Daily Progress",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (totalHabits > 0) {
                        "You've completed $completedCount of $totalHabits habits today."
                    } else {
                        "No habits setup yet. Create your first goal to get started!"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Interactive motivational quote
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "“Consistency beats intensity.”",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberTeal,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Dynamic progress arc
            Box(
                modifier = Modifier.size(84.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { completionPercent },
                    modifier = Modifier.fillMaxSize(),
                    color = NeonPurple,
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(completionPercent * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Done",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionPromptCard(onRequestPermission: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StreakRose.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "Notifications",
                tint = StreakRose,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stay Consistent!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Enable notifications to receive smart daily reminders and maintain your streaks.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = StreakRose),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.testTag("grant_notification_permission_button")
            ) {
                Text("Allow", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun EmptyStateCard(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.TrackChanges,
            contentDescription = "No habits",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your journey starts here.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Track your daily hydration, zen meditation, workout or learning goals to build a reliable habit streak.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
            modifier = Modifier.testTag("create_first_habit_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create First Habit")
        }
    }
}

@Composable
fun HabitItemCard(
    habit: Habit,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) NeonPurple.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle checkbox
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) NeonPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    .clickable { onToggle() }
                    .testTag("habit_checkbox_${habit.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                if (habit.description.isNotEmpty()) {
                    Text(
                        text = habit.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Tag
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (habit.category) {
                                    "Health" -> CyberTeal.copy(alpha = 0.15f)
                                    "Fitness" -> StreakRose.copy(alpha = 0.15f)
                                    "Mindfulness" -> NeonPurple.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = habit.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (habit.category) {
                                "Health" -> CyberTeal
                                "Fitness" -> StreakRose
                                "Mindfulness" -> NeonPurple
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Frequency Tag
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = habit.frequency,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Notification status indicator
                    if (habit.isNotificationEnabled && habit.reminderTime != null) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications active",
                            tint = CyberTeal,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Streak Fire Flame Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = "Streak",
                    tint = if (habit.streak > 0) StreakRose else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "${habit.streak}d",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (habit.streak > 0) StreakRose else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_habit_${habit.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ChartsTab(
    habits: List<Habit>,
    logs: List<HabitLog>,
    milestones: List<StreakMilestone>
) {
    val totalHabits = habits.size
    val totalLogs = logs.size

    val categoryCounts = habits.groupBy { it.category }.mapValues { it.value.size }
    val maxStreak = if (habits.isNotEmpty()) habits.maxOf { it.bestStreak } else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Habit Insights",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Unlock details of your consistency and milestones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Stats Row Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = CyberTeal)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total Logged", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalLogs times", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = StreakRose)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Best Streak", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$maxStreak Days", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // 1. Weekly performance trend cubic Bézier chart
        item {
            WeeklyTrendChart(logs = logs, habitCount = totalHabits)
        }

        // 2. Category Donut Chart
        item {
            CategoryDonutChart(categories = categoryCounts)
        }

        // 3. GitHub style consistency heatmap
        item {
            MonthlyHeatmapGrid(logs = logs)
        }

        // 4. Streak Milestones Section Title
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Milestones",
                        tint = NeonPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Streak Milestones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Celebrate your discipline by unlocking milestone achievements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 5. Streak Milestones List
        if (milestones.isEmpty()) {
            item {
                Text(
                    text = "Initializing milestones...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(milestones, key = { it.milestoneId }) { milestone ->
                MilestoneItemCard(milestone = milestone, currentBestStreak = maxStreak)
            }
        }
    }
}

@Composable
fun MilestoneItemCard(milestone: StreakMilestone, currentBestStreak: Int) {
    val isAchieved = milestone.isAchieved
    val target = milestone.targetStreak
    val progress = if (isAchieved) 1f else (currentBestStreak.toFloat() / target).coerceAtMost(1f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAchieved) NeonPurple.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("milestone_card_${milestone.milestoneId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trophy Badge Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAchieved) {
                            Brush.linearGradient(listOf(NeonPurple, CyberTeal))
                        } else {
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAchieved) Icons.Default.EmojiEvents else Icons.Default.Lock,
                    contentDescription = if (isAchieved) "Unlocked" else "Locked",
                    tint = if (isAchieved) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = milestone.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isAchieved) {
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.linearGradient(listOf(CyberTeal.copy(alpha = 0.2f), NeonPurple.copy(alpha = 0.2f))),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "UNLOCKED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTeal
                            )
                        }
                    } else {
                        Text(
                            text = "$currentBestStreak/$target Days",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = milestone.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (!isAchieved) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonPurple,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                } else if (!milestone.achievedDate.isNullOrEmpty()) {
                    Text(
                        text = "Unlocked on ${milestone.achievedDate}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ManageTab(
    habits: List<Habit>,
    onToggleNotification: (Habit, Boolean) -> Unit,
    onDelete: (Habit) -> Unit,
    themePreferences: ThemePreferences
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "App Appearance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Customize your visual experience.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                val currentTheme by themePreferences.themeMode.collectAsState()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.values().forEach { mode ->
                        val isSelected = currentTheme == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) NeonPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                                .clickable { themePreferences.setThemeMode(mode) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Text(
                    text = "Habit Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Configure and fine-tune your scheduled goals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (habits.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No active habits to manage.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(habits) { habit ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = habit.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = habit.category,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { onDelete(habit) },
                                modifier = Modifier.testTag("manage_delete_${habit.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StreakRose)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = CyberTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Daily Reminder",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    if (habit.reminderTime != null) {
                                        val parseFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                        val displayFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                        val formattedTimes = habit.reminderTime.split(",").map { t ->
                                            try {
                                                parseFormat.parse(t)?.let { displayFormat.format(it) } ?: t
                                            } catch (e: Exception) { t }
                                        }.joinToString(", ")
                                        Text(
                                            text = "Set for $formattedTimes",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = "No reminder time set",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Switch(
                                checked = habit.isNotificationEnabled,
                                onCheckedChange = { enabled -> onToggleNotification(habit, enabled) },
                                modifier = Modifier.testTag("manage_switch_${habit.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, category: String, frequency: String, targetCount: Int, reminderTime: String?, isNotificationEnabled: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Health") }
    var frequency by remember { mutableStateOf("Daily") }
    var reminderTimes by remember { mutableStateOf(listOf<String>()) }
    var currentReminderHour by remember { mutableStateOf("08") }
    var currentReminderMinute by remember { mutableStateOf("00") }
    var currentAmPm by remember { mutableStateOf("AM") }
    var isNotificationEnabled by remember { mutableStateOf(false) }

    val categories = listOf("Health", "Fitness", "Mindfulness", "Productivity", "Custom")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Habit") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    placeholder = { Text("e.g., Read 10 Pages") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_habit_name")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Goal / Motivating Note") },
                    placeholder = { Text("e.g., Learn Compose navigation") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_habit_description")
                )

                // Category selector
                Column {
                    Text("Category", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.take(3).forEach { cat ->
                            val isSelected = category == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) NeonPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                    )
                                    .clickable { category = cat }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.drop(3).forEach { cat ->
                            val isSelected = category == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) NeonPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                    )
                                    .clickable { category = cat }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Reminder section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Daily Reminder", style = MaterialTheme.typography.bodyMedium)
                        Text("Get notified when it is time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isNotificationEnabled,
                        onCheckedChange = { isNotificationEnabled = it },
                        modifier = Modifier.testTag("switch_dialog_notif")
                    )
                }

                if (isNotificationEnabled) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val parseFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        val displayFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                        reminderTimes.forEach { time ->
                            val displayTime = try {
                                parseFormat.parse(time)?.let { displayFormat.format(it) } ?: time
                            } catch(e: Exception) { time }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(displayTime)
                                IconButton(onClick = { reminderTimes = reminderTimes.filter { it != time } }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = currentReminderHour,
                                onValueChange = { if (it.length <= 2 && (it.isEmpty() || (it.toIntOrNull() ?: 0) in 1..12)) currentReminderHour = it },
                                label = { Text("Hour") },
                                modifier = Modifier.width(72.dp).testTag("input_reminder_hour"),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            Text(" : ", fontSize = 24.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            OutlinedTextField(
                                value = currentReminderMinute,
                                onValueChange = { if (it.length <= 2 && (it.isEmpty() || (it.toIntOrNull() ?: 0) in 0..59)) currentReminderMinute = it },
                                label = { Text("Min") },
                                modifier = Modifier.width(72.dp).testTag("input_reminder_minute"),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clickable { currentAmPm = "AM" }
                                        .background(if (currentAmPm == "AM") NeonPurple else Color.Transparent)
                                        .padding(horizontal = 8.dp, vertical = 12.dp)
                                ) {
                                    Text("AM", color = if (currentAmPm == "AM") Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clickable { currentAmPm = "PM" }
                                        .background(if (currentAmPm == "PM") NeonPurple else Color.Transparent)
                                        .padding(horizontal = 8.dp, vertical = 12.dp)
                                ) {
                                    Text("PM", color = if (currentAmPm == "PM") Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { 
                                    if (currentReminderHour.isNotEmpty() && currentReminderMinute.isNotEmpty()) {
                                        val hInt = currentReminderHour.toIntOrNull() ?: 8
                                        var h24 = hInt
                                        if (currentAmPm == "AM" && h24 == 12) h24 = 0
                                        if (currentAmPm == "PM" && h24 < 12) h24 += 12
                                        val h = h24.toString().padStart(2, '0')
                                        val m = currentReminderMinute.padStart(2, '0')
                                        val formattedTime = "$h:$m"
                                        if (formattedTime !in reminderTimes) {
                                            reminderTimes = reminderTimes + formattedTime
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Time", tint = NeonPurple)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty()) {
                        val finalTimes = if (reminderTimes.isEmpty()) {
                            val hInt = currentReminderHour.toIntOrNull() ?: 8
                            var h24 = hInt
                            if (currentAmPm == "AM" && h24 == 12) h24 = 0
                            if (currentAmPm == "PM" && h24 < 12) h24 += 12
                            val h = h24.toString().padStart(2, '0')
                            val m = currentReminderMinute.padStart(2, '0')
                            listOf("$h:$m")
                        } else {
                            reminderTimes
                        }
                        val reminderTimeStr = if (isNotificationEnabled) finalTimes.joinToString(",") else null
                        onConfirm(name, description, category, frequency, 1, reminderTimeStr, isNotificationEnabled)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_button")
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun ProfileMenuSheetContent(
    themePreferences: ThemePreferences,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profile Menu",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Select your avatar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        val currentAvatarIndex by themePreferences.selectedAvatarIndex.collectAsState()
        val avatars = listOf(
            com.example.R.drawable.av1, com.example.R.drawable.av2, com.example.R.drawable.av3,
            com.example.R.drawable.av4, com.example.R.drawable.av5, com.example.R.drawable.av6,
            com.example.R.drawable.av7, com.example.R.drawable.av8, com.example.R.drawable.av9
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            for (row in 0..2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0..2) {
                        val index = row * 3 + col + 1
                        val resId = avatars[index - 1]
                        val isSelected = currentAvatarIndex == index
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) NeonPurple else Color.Transparent)
                                .padding(if (isSelected) 4.dp else 0.dp)
                                .clickable { themePreferences.setAvatarIndex(index) }
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = resId),
                                contentDescription = "Avatar $index",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = StreakRose),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("logout_button")
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Logout")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
