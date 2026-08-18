package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HabitViewModel
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import com.example.ui.AuthState
import com.example.ui.AuthViewModel
import com.example.ui.screens.AuthScreen
import com.example.data.ThemeMode
import com.example.data.ThemePreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OnboardingResult
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import com.example.updater.UpdateDialog
import com.example.updater.UpdateInfo
import com.example.updater.UpdaterApi
import com.example.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val themePreferences = remember { ThemePreferences(context) }
            val currentThemeMode by themePreferences.themeMode.collectAsState()
            val isSystemDark = isSystemInDarkTheme()

            var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }

            LaunchedEffect(Unit) {
                runCatching {
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://habit-tracker-8cb6b.web.app/")
                        .addConverterFactory(MoshiConverterFactory.create())
                        .build()
                    val updaterApi = retrofit.create(UpdaterApi::class.java)

                    val updateInfo = updaterApi.getUpdateInfo("https://habit-tracker-8cb6b.web.app/version.json")
                    if (updateInfo.versionCode > BuildConfig.VERSION_CODE) {
                        availableUpdate = updateInfo
                    }
                }
            }
            
            val darkTheme = when (currentThemeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> isSystemDark
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                val authViewModel: AuthViewModel = viewModel()
                val habitViewModel: HabitViewModel = viewModel()
                
                val authState by authViewModel.authState.collectAsState()
                val isOnboardingComplete by themePreferences.isOnboardingComplete.collectAsState()
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (authState) {
                        is AuthState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        is AuthState.NeedsRegistration -> {
                            AuthScreen(viewModel = authViewModel, isRegistration = true)
                        }
                        is AuthState.NeedsLogin -> {
                            AuthScreen(viewModel = authViewModel, isRegistration = false)
                        }
                        is AuthState.Authenticated -> {
                            val user = (authState as AuthState.Authenticated).user
                            
                            var checkingOnboarding by remember { mutableStateOf(!isOnboardingComplete) }
                            val coroutineScope = rememberCoroutineScope()

                            LaunchedEffect(user.uid) {
                                habitViewModel.checkAndRestoreFromCloud()
                                if (!isOnboardingComplete) {
                                    val isCompleteInCloud = authViewModel.checkIsOnboardingComplete()
                                    if (isCompleteInCloud) {
                                        themePreferences.setOnboardingComplete(true)
                                    }
                                }
                                checkingOnboarding = false
                            }

                            if (checkingOnboarding) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            } else if (!isOnboardingComplete) {
                                val isGoogleUser = user.providerData.any { it.providerId == "google.com" }
                                OnboardingScreen(
                                    initialDisplayName = user.displayName ?: "",
                                    isGoogleUser = isGoogleUser,
                                    onComplete = { result: OnboardingResult ->
                                        // Save profile data
                                        themePreferences.setGender(result.gender)
                                        themePreferences.setProfession(result.profession)
                                        themePreferences.setAvatarIndex(result.avatarIndex)
                                        
                                        // Create selected habits
                                        result.selectedHabits.forEach { habitName ->
                                            habitViewModel.addHabit(
                                                name = habitName,
                                                description = "Auto-created during onboarding",
                                                category = result.profession,
                                                frequency = "daily",
                                                targetCount = 1,
                                                reminderTime = null,
                                                isNotificationEnabled = false
                                            )
                                        }
                                        
                                        coroutineScope.launch {
                                            if (isGoogleUser && result.displayName.isNotBlank() && result.displayName != user.displayName) {
                                                authViewModel.updateDisplayName(result.displayName)
                                            }
                                            authViewModel.markOnboardingComplete()
                                            themePreferences.setOnboardingComplete(true)
                                        }
                                    }
                                )
                            } else {
                                MainDashboard(
                                    viewModel = habitViewModel,
                                    modifier = Modifier.fillMaxSize(),
                                    onLogout = { authViewModel.logout() },
                                    userName = user.displayName ?: "User",
                                    themePreferences = themePreferences
                                )
                            }
                        }
                    }

                    availableUpdate?.let { updateInfo ->
                        UpdateDialog(
                            updateInfo = updateInfo,
                            onDismiss = { availableUpdate = null }
                        )
                    }
                }
            }
        }
    }
}
