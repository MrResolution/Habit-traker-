package com.example.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    LIGHT, DARK, AUTO
}

class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.AUTO.name) ?: ThemeMode.AUTO.name)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _selectedAvatarIndex = MutableStateFlow(
        prefs.getInt("selected_avatar_index", 1)
    )
    val selectedAvatarIndex: StateFlow<Int> = _selectedAvatarIndex.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit {
            putString("theme_mode", mode.name)
        }
        _themeMode.value = mode
    }

    fun setAvatarIndex(index: Int) {
        prefs.edit {
            putInt("selected_avatar_index", index)
        }
        _selectedAvatarIndex.value = index
    }

    // Onboarding
    private val _isOnboardingComplete = MutableStateFlow(
        prefs.getBoolean("onboarding_complete", false)
    )
    val isOnboardingComplete: StateFlow<Boolean> = _isOnboardingComplete.asStateFlow()

    fun setOnboardingComplete(complete: Boolean) {
        prefs.edit {
            putBoolean("onboarding_complete", complete)
        }
        _isOnboardingComplete.value = complete
    }

    private val _gender = MutableStateFlow(
        prefs.getString("user_gender", "") ?: ""
    )
    val gender: StateFlow<String> = _gender.asStateFlow()

    fun setGender(gender: String) {
        prefs.edit {
            putString("user_gender", gender)
        }
        _gender.value = gender
    }

    private val _profession = MutableStateFlow(
        prefs.getString("user_profession", "") ?: ""
    )
    val profession: StateFlow<String> = _profession.asStateFlow()

    fun setProfession(profession: String) {
        prefs.edit {
            putString("user_profession", profession)
        }
        _profession.value = profession
    }

    private val _isSoundEnabled = MutableStateFlow(
        prefs.getBoolean("sound_effects_enabled", true)
    )
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean("sound_effects_enabled", enabled)
        }
        _isSoundEnabled.value = enabled
    }
}
