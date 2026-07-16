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
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

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

    private val _isAutoBackupEnabled = MutableStateFlow(
        prefs.getBoolean("auto_backup_enabled", true)
    )
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean("auto_backup_enabled", enabled)
        }
        _isAutoBackupEnabled.value = enabled
    }
}
