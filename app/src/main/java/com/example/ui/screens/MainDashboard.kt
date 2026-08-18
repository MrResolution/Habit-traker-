package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
import com.example.ui.components.SkeletonChartCard
import com.example.ui.components.SkeletonHabitCard
import com.example.ui.components.SkeletonHeaderSection
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
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
    val scoreBreakdown by viewModel.scoreBreakdown.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var activeTab by rememberSaveable { mutableStateOf("today") } // "today", "charts", "leaderboard", "manage"
    var showAddHabitDialog by rememberSaveable { mutableStateOf(false) }
    var showProfileSheet by rememberSaveable { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTodayDate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Hi, $userName",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .background(NeonPurple.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Lvl ${scoreBreakdown.levelInfo.level}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonPurple
                                    )
                                }
                            }
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
                                    text = "$totalStreak Day Streak • ${scoreBreakdown.totalScore} pts",
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
            val isSoundEnabled by themePreferences.isSoundEnabled.collectAsState()

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
                    onDelete = { habit -> viewModel.deleteHabit(habit) },
                    onAddClick = { showAddHabitDialog = true },
                    isLoading = isLoading,
                    isSoundEnabled = isSoundEnabled
                )
                "charts" -> ChartsTab(
                    habits = habits,
                    logs = logs,
                    milestones = milestones,
                    isLoading = isLoading
                )
                "leaderboard" -> LeaderboardTab(
                    leaderboardEntries = leaderboardEntries,
                    isLoading = isLoading
                )
                "manage" -> ManageTab(
                    habits = habits,
                    onToggleNotification = { habit, enabled ->
                        viewModel.updateHabit(habit.copy(isNotificationEnabled = enabled))
                    },
                    onUpdateHabit = { updatedHabit ->
                        viewModel.updateHabit(updatedHabit)
                    },
                    onDelete = { habit -> viewModel.deleteHabit(habit) },
                    themePreferences = themePreferences
                )
            }

            if (showAddHabitDialog) {
                AddHabitDialog(
                    onDismiss = { showAddHabitDialog = false },
                    onConfirm = { name, desc, category, freq, target, reminder, notifEnabled ->
                        viewModel.addHabit(name, desc, category, freq, target, reminder, notifEnabled)
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
    onAddClick: () -> Unit,
    isLoading: Boolean = false,
    isSoundEnabled: Boolean = true
) {
    if (isLoading) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                SkeletonHeaderSection()
            }
            items(4) {
                SkeletonHabitCard()
            }
        }
        return
    }

    val logsToday = remember(logs, selectedDate) { logs.filter { it.date == selectedDate } }
    val completedIds = remember(logsToday) { logsToday.map { it.habitId }.toSet() }

    val totalHabits = habits.size
    val completedCount = remember(habits, completedIds) { habits.count { completedIds.contains(it.id) } }
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
                totalHabits = totalHabits,
                selectedDate = selectedDate
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
                val isAllCompletedAfterThis = completedCount + 1 == totalHabits && !isCompleted
                HabitItemCard(
                    habit = habit,
                    isCompleted = isCompleted,
                    onToggle = onToggle,
                    onDelete = onDelete,
                    isSoundEnabled = isSoundEnabled,
                    isAllCompletedAfterThis = isAllCompletedAfterThis
                )
            }
        }
    }
}

@Composable
fun SmoothComplimentText(
    text: String,
    modifier: Modifier = Modifier
) {
    var startAnim by remember { mutableStateOf(false) }

    LaunchedEffect(text) {
        startAnim = true
    }

    val offsetY by animateFloatAsState(
        targetValue = if (startAnim) 0f else 60f,
        animationSpec = tween(
            durationMillis = 650,
            easing = FastOutSlowInEasing
        ),
        label = "smoothOffsetY"
    )

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.85f,
        animationSpec = tween(
            durationMillis = 650,
            easing = FastOutSlowInEasing
        ),
        label = "smoothScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(
            durationMillis = 450,
            easing = LinearEasing
        ),
        label = "smoothAlpha"
    )

    Text(
        text = text,
        fontSize = 42.sp,
        fontWeight = FontWeight.Black,
        color = Color.White,
        textAlign = TextAlign.Center,
        letterSpacing = 2.sp,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = offsetY
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeaderSection(
    completionPercent: Float,
    completedCount: Int,
    totalHabits: Int,
    selectedDate: String = ""
) {
    val is100Percent = completionPercent >= 1.0f && totalHabits > 0
    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }
    }

    val soundManager = remember(context) { com.example.util.SoundManager.getInstance(context) }

    fun startContinuousVibration() {
        try {
            view.isHapticFeedbackEnabled = true
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val timings = longArrayOf(0, 15, 15)
                        val amplitudes = intArrayOf(0, 140, 220)
                        val effect = android.os.VibrationEffect.createWaveform(timings, amplitudes, 0)
                        v.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(longArrayOf(0, 100, 100), 0)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    fun stopVibration(isSuccess: Boolean) {
        try {
            vibrator?.cancel()
            if (isSuccess) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                soundManager.playReward(true, view)
                vibrator?.let { v ->
                    if (v.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val effect = android.os.VibrationEffect.createOneShot(220L, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                            v.vibrate(effect)
                        } else {
                            @Suppress("DEPRECATION")
                            v.vibrate(220L)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    val dailyCompliment = remember(selectedDate) {
        val compliments = listOf(
            "UNSTOPPABLE",
            "LEGENDARY",
            "UNMATCHED",
            "FLAWLESS",
            "PURE GENIUS",
            "PHENOMENAL",
            "CHAMPION",
            "VICTORIOUS",
            "MASTERPIECE",
            "PERFECTION"
        )
        val dateKey = if (selectedDate.isNotEmpty()) {
            selectedDate
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
        val index = kotlin.math.abs(dateKey.hashCode()) % compliments.size
        compliments[index]
    }

    var isHolding by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(is100Percent) {
        if (!is100Percent) {
            isUnlocked = false
            isHolding = false
            holdProgress = 0f
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "HeaderColorAnimation")
    val hueShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hueShift"
    )

    val animColor1 = remember(hueShift) { Color.hsv(hueShift % 360f, 0.75f, 0.95f) }
    val animColor2 = remember(hueShift) { Color.hsv((hueShift + 120f) % 360f, 0.75f, 0.95f) }
    val animColor3 = remember(hueShift) { Color.hsv((hueShift + 240f) % 360f, 0.75f, 0.95f) }

    val cardBrush = if (isUnlocked || isHolding) {
        Brush.linearGradient(
            colors = listOf(
                animColor1.copy(alpha = 0.55f),
                animColor2.copy(alpha = 0.45f),
                animColor3.copy(alpha = 0.55f)
            )
        )
    } else if (is100Percent) {
        Brush.linearGradient(
            colors = listOf(
                NeonPurple.copy(alpha = 0.35f),
                CyberTeal.copy(alpha = 0.25f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                NeonPurple.copy(alpha = 0.25f),
                CyberTeal.copy(alpha = 0.1f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(cardBrush)
            .then(
                if (is100Percent && !isUnlocked) {
                    Modifier.pointerInput(is100Percent, isUnlocked) {
                        detectTapGestures(
                            onPress = {
                                isHolding = true
                                holdProgress = 0f
                                val startTime = System.currentTimeMillis()
                                val holdDuration = 1500L
                                var completed = false

                                startContinuousVibration()
                                soundManager.startProcessSound(true, view)

                                val job = coroutineScope.launch {
                                    val step = 30L
                                    while (isActive) {
                                        delay(step)
                                        val elapsed = System.currentTimeMillis() - startTime
                                        holdProgress = (elapsed.toFloat() / holdDuration).coerceIn(0f, 1f)

                                        if (elapsed % 150L < step) {
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                        }

                                        if (elapsed >= holdDuration) {
                                            completed = true
                                            holdProgress = 1f
                                            stopVibration(isSuccess = true)
                                            isUnlocked = true
                                            isHolding = false
                                            break
                                        }
                                    }
                                }

                                try {
                                    awaitRelease()
                                } finally {
                                    if (!completed) {
                                        job.cancel()
                                        soundManager.stopProcessSound()
                                        stopVibration(isSuccess = false)
                                        isHolding = false
                                        holdProgress = 0f
                                    }
                                }
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        if (isHolding && !isUnlocked) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
            ) {
                val width = size.width
                val height = size.height
                val fogRadius = (width * 0.9f) * (0.35f + holdProgress * 0.65f)
                val alphaDensity = (0.35f + holdProgress * 0.6f).coerceIn(0f, 0.95f)

                val angleRad = Math.toRadians((hueShift * 2.5).toDouble())
                val offsetX1 = width * 0.35f + (Math.cos(angleRad) * width * 0.18f).toFloat()
                val offsetY1 = height * 0.45f + (Math.sin(angleRad) * height * 0.22f).toFloat()

                val offsetX2 = width * 0.65f - (Math.cos(angleRad) * width * 0.18f).toFloat()
                val offsetY2 = height * 0.55f - (Math.sin(angleRad) * height * 0.22f).toFloat()

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animColor1.copy(alpha = alphaDensity),
                            animColor2.copy(alpha = alphaDensity * 0.7f),
                            Color.Transparent
                        ),
                        center = Offset(offsetX1, offsetY1),
                        radius = fogRadius
                    )
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animColor3.copy(alpha = alphaDensity * 0.85f),
                            animColor1.copy(alpha = alphaDensity * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(offsetX2, offsetY2),
                        radius = fogRadius * 0.9f
                    )
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = alphaDensity * 0.45f),
                            animColor2.copy(alpha = alphaDensity * 0.65f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, height * 0.5f),
                        radius = fogRadius * 1.15f
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp)
                .padding(24.dp)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) {
                AnimatedVisibility(
                    visible = isUnlocked,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight * 2 },
                        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(durationMillis = 500)),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> -fullHeight }
                    ) + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clickable {
                                soundManager.playReward(true, view)
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                            }
                            .testTag("unlocked_compliment_section"),
                        contentAlignment = Alignment.Center
                    ) {
                        SmoothComplimentText(
                            text = dailyCompliment,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("compliment_rolling_text")
                        )
                    }
                }
            } else if (!isHolding) {
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

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (is100Percent) {
                                        Brush.horizontalGradient(listOf(NeonPurple.copy(alpha = 0.3f), CyberTeal.copy(alpha = 0.3f)))
                                    } else {
                                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)))
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("hold_me_dialog_badge")
                        ) {
                            Text(
                                text = if (is100Percent) "“Click me!”" else "“Consistency beats intensity.”",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (is100Percent) NeonPurple else CyberTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier.size(84.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { completionPercent },
                            modifier = Modifier.fillMaxSize(),
                            color = if (is100Percent) CyberTeal else NeonPurple,
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
    onToggle: (Int) -> Unit,
    onDelete: (Habit) -> Unit,
    isSoundEnabled: Boolean = true,
    isAllCompletedAfterThis: Boolean = false
) {
    val context = LocalContext.current
    val soundManager = remember(context) { com.example.util.SoundManager.getInstance(context) }
    val haptic = LocalHapticFeedback.current

    val checkScale by animateFloatAsState(
        targetValue = if (isCompleted) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkScale"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) NeonPurple.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCompleted) NeonPurple.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                    .size(32.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                    }
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) NeonPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    .clickable {
                        if (!isCompleted) {
                            if (isAllCompletedAfterThis) {
                                soundManager.playTaskCompleteAll(isSoundEnabled)
                            } else {
                                soundManager.playTaskCheck(isSoundEnabled)
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else {
                            soundManager.playTaskUncheck(isSoundEnabled)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onToggle(habit.id)
                    }
                    .testTag("habit_checkbox_${habit.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
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
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
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
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
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
                            modifier = Modifier.size(14.dp)
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
                    .padding(horizontal = 10.dp, vertical = 6.dp)
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
                onClick = { onDelete(habit) },
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
    milestones: List<StreakMilestone>,
    isLoading: Boolean = false
) {
    if (isLoading) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(3) {
                SkeletonChartCard()
            }
        }
        return
    }
    val totalHabits = habits.size
    val totalLogs = logs.size
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var screenTimeStr by remember { mutableStateOf("Calculating...") }
    var hasUsagePermission by remember { mutableStateOf(true) }

    suspend fun checkAndFetchScreenTime() {
        val permissionGranted = hasUsageStatsPermission(context)
        hasUsagePermission = permissionGranted
        if (!permissionGranted) {
            screenTimeStr = "Permission Required"
        } else {
            val totalTime = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                getTodayScreenTimeMillis(context)
            }
            val hours = totalTime / (1000 * 60 * 60)
            val minutes = (totalTime / (1000 * 60)) % 60
            screenTimeStr = "${hours}h ${minutes}m"
        }
    }

    LaunchedEffect(Unit) {
        checkAndFetchScreenTime()
    }

    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch { checkAndFetchScreenTime() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

        // Screen Time Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = NeonPurple)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Screen Time Today", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(screenTimeStr, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        if (!hasUsagePermission) {
                            Text("Requires usage access permission", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (!hasUsagePermission) {
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallbackIntent = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(fallbackIntent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                        ) {
                            Text("Grant", fontSize = 12.sp)
                        }
                    }
                }
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
    onUpdateHabit: (Habit) -> Unit,
    onDelete: (Habit) -> Unit,
    themePreferences: ThemePreferences
) {
    var habitToEditTime by remember { mutableStateOf<Habit?>(null) }

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
            val isSoundEnabled by themePreferences.isSoundEnabled.collectAsState()
            Column {
                Text(
                    text = "Audio & Feedback",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sound effects and tactile feedback settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NeonPurple.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Sound Effects",
                                    tint = NeonPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Task Sound Effects (SFX)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Play audio feedback when checking off tasks",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isSoundEnabled,
                            onCheckedChange = { themePreferences.setSoundEnabled(it) },
                            modifier = Modifier.testTag("sfx_toggle_switch")
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

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
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { habitToEditTime = habit }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Time",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
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

    if (habitToEditTime != null) {
        EditReminderDialog(
            initialReminderTime = habitToEditTime!!.reminderTime,
            onDismiss = { habitToEditTime = null },
            onConfirm = { newTime ->
                onUpdateHabit(habitToEditTime!!.copy(reminderTime = newTime))
                habitToEditTime = null
            }
        )
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
    var customCategory by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Daily") }
    var reminderTimes by remember { mutableStateOf(listOf<String>()) }
    var currentReminderHour by remember { mutableStateOf("08") }
    var currentReminderMinute by remember { mutableStateOf("00") }
    var currentAmPm by remember { mutableStateOf("AM") }
    var isNotificationEnabled by remember { mutableStateOf(false) }

    val categoriesWithIcons = listOf(
        Triple("Health", "❤️", "Health"),
        Triple("Fitness", "🏋️", "Fitness"),
        Triple("Mindfulness", "🧘", "Mindfulness"),
        Triple("Productivity", "⚡", "Productivity"),
        Triple("Custom", "✨", "Custom")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "New Habit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    placeholder = { Text("e.g. Read 15 pages") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_habit_name")
                )

                // Goal/Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Motivating Goal") },
                    placeholder = { Text("e.g. Expand knowledge daily") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_habit_description")
                )

                // Minimalist Category Pills
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoriesWithIcons.forEach { (catKey, emoji, label) ->
                            val isSelected = category == catKey
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) NeonPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                border = if (isSelected) BorderStroke(1.dp, NeonPurple) else null,
                                modifier = Modifier.clickable { category = catKey }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(emoji, fontSize = 13.sp)
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = category == "Custom") {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text("Custom Category Name") },
                        placeholder = { Text("e.g. Learning") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Frequency segment
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Frequency", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .padding(3.dp)
                    ) {
                        listOf("Daily", "Weekly").forEach { freq ->
                            val isSelected = frequency == freq
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSelected) NeonPurple else Color.Transparent)
                                    .clickable { frequency = freq }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = freq,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Daily Reminder Switch & Time Picker
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Reminder", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Receive push notifications", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = { isNotificationEnabled = it },
                            modifier = Modifier.testTag("switch_dialog_notif")
                        )
                    }

                    if (isNotificationEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Quick Presets
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(Triple("08:00", "🌅 08:00 AM", "08"), Triple("13:00", "☀️ 01:00 PM", "01"), Triple("20:00", "🌙 08:00 PM", "08")).forEach { (t24, label, hStr) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (t24 in reminderTimes) CyberTeal.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (t24 !in reminderTimes) reminderTimes = reminderTimes + t24
                                            }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Added times chips
                            val parseFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            val displayFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                            reminderTimes.forEach { time ->
                                val displayTime = try {
                                    parseFormat.parse(time)?.let { displayFormat.format(it) } ?: time
                                } catch (e: Exception) { time }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(displayTime, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        IconButton(onClick = { reminderTimes = reminderTimes.filter { it != time } }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            // Input fields matching test tags
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = currentReminderHour,
                                    onValueChange = { if (it.length <= 2 && (it.isEmpty() || (it.toIntOrNull() ?: 0) in 0..12)) currentReminderHour = it },
                                    label = { Text("HH") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.width(68.dp).testTag("input_reminder_hour"),
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                                Text(" : ", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                OutlinedTextField(
                                    value = currentReminderMinute,
                                    onValueChange = { if (it.length <= 2 && (it.isEmpty() || (it.toIntOrNull() ?: 0) in 0..59)) currentReminderMinute = it },
                                    label = { Text("MM") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.width(68.dp).testTag("input_reminder_minute"),
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clickable { currentAmPm = "AM" }
                                            .background(if (currentAmPm == "AM") NeonPurple else Color.Transparent)
                                            .padding(horizontal = 6.dp, vertical = 10.dp)
                                    ) {
                                        Text("AM", color = if (currentAmPm == "AM") Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clickable { currentAmPm = "PM" }
                                            .background(if (currentAmPm == "PM") NeonPurple else Color.Transparent)
                                            .padding(horizontal = 6.dp, vertical = 10.dp)
                                    ) {
                                        Text("PM", color = if (currentAmPm == "PM") Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { 
                                        if (currentReminderHour.isNotEmpty() && currentReminderMinute.isNotEmpty()) {
                                            var hInt = currentReminderHour.toIntOrNull() ?: 8
                                            if (hInt == 0) hInt = 12
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
                                    Icon(Icons.Default.AddCircle, contentDescription = "Add Time", tint = NeonPurple)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val finalTimes = if (reminderTimes.isEmpty()) {
                            var hInt = currentReminderHour.toIntOrNull() ?: 8
                            if (hInt == 0) hInt = 12
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
                        val finalCategory = if (category == "Custom" && customCategory.isNotBlank()) customCategory.trim() else category
                        onConfirm(name.trim(), description.trim(), finalCategory, frequency, 1, reminderTimeStr, isNotificationEnabled)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text("Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("dialog_cancel_button")
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
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
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun EditReminderDialog(
    initialReminderTime: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var reminderTimes by remember { mutableStateOf(initialReminderTime?.split(",")?.filter { it.isNotBlank() } ?: listOf<String>()) }
    var currentReminderHour by remember { mutableStateOf("08") }
    var currentReminderMinute by remember { mutableStateOf("00") }
    var currentAmPm by remember { mutableStateOf("AM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Reminder Time") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
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
                        onValueChange = { if (it.length <= 2 && (it.isEmpty() || (it.toIntOrNull() ?: 0) in 0..12)) currentReminderHour = it },
                        label = { Text("Hour") },
                        modifier = Modifier.width(72.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Text(" : ", fontSize = 24.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    OutlinedTextField(
                        value = currentReminderMinute,
                        onValueChange = { if (it.length <= 2 && (it.isEmpty() || (it.toIntOrNull() ?: 0) in 0..59)) currentReminderMinute = it },
                        label = { Text("Min") },
                        modifier = Modifier.width(72.dp),
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
                                var hInt = currentReminderHour.toIntOrNull() ?: 8
                                if (hInt == 0) hInt = 12
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
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTimes = if (reminderTimes.isEmpty() && currentReminderHour.isNotEmpty() && currentReminderMinute.isNotEmpty()) {
                        var hInt = currentReminderHour.toIntOrNull() ?: 8
                        if (hInt == 0) hInt = 12
                        var h24 = hInt
                        if (currentAmPm == "AM" && h24 == 12) h24 = 0
                        if (currentAmPm == "PM" && h24 < 12) h24 += 12
                        val h = h24.toString().padStart(2, '0')
                        val m = currentReminderMinute.padStart(2, '0')
                        listOf("$h:$m")
                    } else {
                        reminderTimes
                    }
                    onConfirm(if (finalTimes.isEmpty()) null else finalTimes.joinToString(","))
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun hasUsageStatsPermission(context: android.content.Context): Boolean {
    val appOps = context.getSystemService(android.content.Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
        ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }
    return mode == android.app.AppOpsManager.MODE_ALLOWED
}

private fun getTodayScreenTimeMillis(context: android.content.Context): Long {
    val usageStatsManager = context.getSystemService(android.content.Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
        ?: return 0L

    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startTime = cal.timeInMillis
    val endTime = System.currentTimeMillis()

    if (endTime <= startTime) return 0L
    val maxPossibleTime = endTime - startTime

    val ignoredPackages = setOf(
        "com.android.systemui",
        "android",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.android.settings.intelligence"
    )

    // Event type constants
    val EVENT_ACTIVITY_RESUMED = 1
    val EVENT_ACTIVITY_PAUSED = 2
    val EVENT_SCREEN_INTERACTIVE = 15
    val EVENT_SCREEN_NON_INTERACTIVE = 16
    val EVENT_KEYGUARD_SHOWN = 17

    var totalTime = 0L
    var isScreenOn = true
    var currentApp: String? = null
    var appStartTime: Long? = null

    try {
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        if (usageEvents != null) {
            val event = android.app.usage.UsageEvents.Event()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val pkg = event.packageName ?: ""
                val type = event.eventType
                val timeStamp = event.timeStamp

                when (type) {
                    EVENT_SCREEN_NON_INTERACTIVE, EVENT_KEYGUARD_SHOWN -> {
                        if (isScreenOn && appStartTime != null) {
                            if (timeStamp > appStartTime) {
                                totalTime += (timeStamp - appStartTime)
                            }
                        }
                        isScreenOn = false
                        currentApp = null
                        appStartTime = null
                    }
                    EVENT_SCREEN_INTERACTIVE -> {
                        isScreenOn = true
                        if (currentApp != null) {
                            appStartTime = timeStamp
                        }
                    }
                    EVENT_ACTIVITY_RESUMED -> {
                        if (!ignoredPackages.contains(pkg)) {
                            if (isScreenOn && currentApp != null && appStartTime != null) {
                                if (timeStamp > appStartTime) {
                                    totalTime += (timeStamp - appStartTime)
                                }
                            }
                            currentApp = pkg
                            appStartTime = if (isScreenOn) timeStamp else null
                        }
                    }
                    EVENT_ACTIVITY_PAUSED -> {
                        if (pkg == currentApp) {
                            if (isScreenOn && appStartTime != null) {
                                if (timeStamp > appStartTime) {
                                    totalTime += (timeStamp - appStartTime)
                                }
                            }
                            currentApp = null
                            appStartTime = null
                        }
                    }
                }
            }

            if (isScreenOn && currentApp != null && appStartTime != null) {
                if (endTime > appStartTime) {
                    totalTime += (endTime - appStartTime)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (totalTime == 0L) {
        try {
            val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
            if (!statsMap.isNullOrEmpty()) {
                for ((pkg, stat) in statsMap) {
                    if (ignoredPackages.contains(pkg)) continue
                    if (stat.totalTimeInForeground > 0) {
                        totalTime += stat.totalTimeInForeground
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return if (totalTime > maxPossibleTime) maxPossibleTime else totalTime
}
