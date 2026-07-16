package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.NeonPurple

data class OnboardingResult(
    val displayName: String,
    val gender: String,
    val profession: String,
    val selectedHabits: List<String>,
    val avatarIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    initialDisplayName: String,
    isGoogleUser: Boolean,
    onComplete: (OnboardingResult) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = if (isGoogleUser) 3 else 2 // Google users get a display name step

    // State
    var displayName by remember { mutableStateOf(initialDisplayName) }
    var selectedProfession by remember { mutableStateOf("") }
    var selectedHabits by remember { mutableStateOf(setOf<String>()) }
    var avatarIndex by remember { mutableStateOf(1) }

    // Progress: starts at 20% (account created), fills to 100%
    val progressFraction = 0.20f + (0.80f * (currentStep.toFloat() / totalSteps.toFloat()))

    MaterialTheme(colorScheme = lightColorScheme()) {
        Scaffold(
            containerColor = Color(0xFFF8F7FC)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Setting up your profile",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = "${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val animatedProgress by animateFloatAsState(
                        targetValue = progressFraction,
                        animationSpec = tween(500)
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NeonPurple,
                        trackColor = NeonPurple.copy(alpha = 0.12f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Content Area (animated)
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) togetherWith
                                slideOutHorizontally(tween(350)) { -it } + fadeOut(tween(350))
                    },
                    modifier = Modifier.weight(1f)
                ) { step ->
                    val actualStep = if (isGoogleUser) step else step + 1 // skip name step for email users
                    when (actualStep) {
                        0 -> DisplayNameStep(
                            displayName = displayName,
                            onNameChange = { displayName = it }
                        )
                        1 -> ProfessionStep(
                            selectedProfession = selectedProfession,
                            onProfessionSelected = { selectedProfession = it }
                        )
                        2 -> HabitSuggestionsStep(
                            profession = selectedProfession,
                            selectedHabits = selectedHabits,
                            onToggleHabit = { habit ->
                                selectedHabits = if (habit in selectedHabits) {
                                    selectedHabits - habit
                                } else {
                                    selectedHabits + habit
                                }
                            }
                        )
                    }
                }

                // Bottom Navigation Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(52.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = NeonPurple)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Back", color = NeonPurple, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    val isLastStep = currentStep == totalSteps - 1
                    val canProceed = when {
                        isGoogleUser && currentStep == 0 -> displayName.isNotBlank()
                        (!isGoogleUser && currentStep == 0) || (isGoogleUser && currentStep == 1) -> selectedProfession.isNotBlank()
                        else -> true
                    }

                    Button(
                        onClick = {
                            if (isLastStep) {
                                onComplete(
                                    OnboardingResult(
                                        displayName = displayName,
                                        gender = "",
                                        profession = selectedProfession,
                                        selectedHabits = selectedHabits.toList(),
                                        avatarIndex = avatarIndex
                                    )
                                )
                            } else {
                                currentStep++
                            }
                        },
                        enabled = canProceed,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text(
                            text = if (isLastStep) "Let's Go!" else "Continue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isLastStep) Icons.Default.Celebration else Icons.Default.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

// ─── Step 0: Display Name (Google users only) ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayNameStep(displayName: String, onNameChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Badge,
            contentDescription = null,
            tint = NeonPurple,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "What should we call you?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose a display name that other users will see.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = displayName,
            onValueChange = onNameChange,
            label = { Text("Display Name") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonPurple,
                cursorColor = NeonPurple,
                focusedLabelColor = NeonPurple
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Step 1: Gender Selection ───────────────────────────────────────────

@Composable
fun GenderStep(selectedGender: String, onGenderSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.People,
            contentDescription = null,
            tint = NeonPurple,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Choose your avatar style",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This helps us pick the right avatar set for you.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        val genderOptions = listOf(
            Triple("Male", Icons.Default.Male, "🙋‍♂️"),
            Triple("Female", Icons.Default.Female, "🙋‍♀️"),
            Triple("Other", Icons.Default.Person, "🧑")
        )

        genderOptions.forEach { (label, icon, _) ->
            SelectableOptionCard(
                label = label,
                icon = icon,
                isSelected = selectedGender == label,
                onClick = { onGenderSelected(label) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ─── Step 2: Profession Selection ───────────────────────────────────────

@Composable
fun ProfessionStep(selectedProfession: String, onProfessionSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Work,
            contentDescription = null,
            tint = NeonPurple,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "What do you do?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We'll suggest habits tailored to your profession.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        val professions = listOf(
            Pair("Engineer", Icons.Default.Computer),
            Pair("Doctor", Icons.Default.LocalHospital),
            Pair("Student", Icons.Default.School),
            Pair("Other", Icons.Default.MoreHoriz)
        )

        professions.forEach { (label, icon) ->
            SelectableOptionCard(
                label = label,
                icon = icon,
                isSelected = selectedProfession == label,
                onClick = { onProfessionSelected(label) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ─── Step 3: Habit Suggestions ──────────────────────────────────────────

@Composable
fun HabitSuggestionsStep(
    profession: String,
    selectedHabits: Set<String>,
    onToggleHabit: (String) -> Unit
) {
    val suggestedHabits = when (profession) {
        "Engineer" -> listOf(
            "💻 1 Hour Coding Practice" to "Sharpen your skills daily",
            "📖 Read Tech Articles" to "Stay updated with trends",
            "🏋️ 30 Min Workout" to "Stay physically active",
            "🧘 10 Min Meditation" to "Clear your mind",
            "💧 Drink 8 Glasses of Water" to "Stay hydrated",
            "📝 Write Daily Journal" to "Reflect on your day"
        )
        "Doctor" -> listOf(
            "📚 Medical Journal Review" to "Stay updated with research",
            "🏃 Morning Run" to "Build cardiovascular health",
            "🧘 15 Min Mindfulness" to "Manage stress effectively",
            "💧 Drink 8 Glasses of Water" to "Stay hydrated",
            "😴 Sleep 7+ Hours" to "Prioritize rest and recovery",
            "📝 Patient Notes Review" to "End-of-day reflection"
        )
        "Student" -> listOf(
            "📖 2 Hours Focused Study" to "Deep learning sessions",
            "📝 Review Class Notes" to "Reinforce daily lessons",
            "🏋️ 30 Min Exercise" to "Keep your body active",
            "💧 Drink 8 Glasses of Water" to "Stay hydrated",
            "🧘 10 Min Meditation" to "Improve focus and clarity",
            "😴 Sleep 8 Hours" to "Rest for peak performance"
        )
        else -> listOf(
            "🏃 Morning Walk" to "Start your day right",
            "📖 Read 30 Minutes" to "Expand your knowledge",
            "💧 Drink 8 Glasses of Water" to "Stay hydrated",
            "🧘 10 Min Meditation" to "Practice mindfulness",
            "📝 Write Daily Journal" to "Reflect and grow",
            "😴 Sleep 7+ Hours" to "Quality rest matters"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Checklist,
            contentDescription = null,
            tint = NeonPurple,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Pick your starter habits",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We've curated these for you. Select the ones you'd like to track!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        suggestedHabits.forEach { (habit, description) ->
            HabitSuggestionCard(
                habitName = habit,
                description = description,
                isSelected = habit in selectedHabits,
                onToggle = { onToggleHabit(habit) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

// ─── Reusable Components ────────────────────────────────────────────────

@Composable
fun SelectableOptionCard(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
@Composable
fun SelectableOptionCard(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) NeonPurple.copy(alpha = 0.08f) else Color.White)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) NeonPurple else Color(0xFFF3F4F6),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isSelected) NeonPurple.copy(alpha = 0.15f) else Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) NeonPurple else Color(0xFF6B7280),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) NeonPurple else Color(0xFF1F2937)
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = NeonPurple,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun HabitSuggestionCard(
    habitName: String,
    description: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) CyberTeal.copy(alpha = 0.06f) else Color.White)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) CyberTeal else Color(0xFFF3F4F6),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habitName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color(0xFF0F766E) else Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF)
            )
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = CyberTeal,
                uncheckedColor = Color(0xFFD1D5DB)
            )
        )
    }
}
