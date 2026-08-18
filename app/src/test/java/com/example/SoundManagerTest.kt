package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.ThemePreferences
import com.example.util.SoundManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SoundManagerTest {

    @Test
    fun testSoundManagerInstance_isNotNull() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = SoundManager.getInstance(context)
        assertNotNull(manager)
    }

    @Test
    fun testSoundManager_playTaskCheck_doesNotCrash() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = SoundManager.getInstance(context)
        // Should execute smoothly without throwing exceptions
        manager.playTaskCheck(enabled = true)
        manager.playTaskCheck(enabled = false)
    }

    @Test
    fun testSoundManager_playTaskUncheck_doesNotCrash() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = SoundManager.getInstance(context)
        manager.playTaskUncheck(enabled = true)
        manager.playTaskUncheck(enabled = false)
    }

    @Test
    fun testSoundManager_playTaskCompleteAll_doesNotCrash() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = SoundManager.getInstance(context)
        manager.playTaskCompleteAll(enabled = true)
        manager.playTaskCompleteAll(enabled = false)
    }

    @Test
    fun testThemePreferences_soundSettingToggle() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val themePrefs = ThemePreferences(context)
        
        // Defaults to true
        assertTrue(themePrefs.isSoundEnabled.value)
        
        // Toggle sound off
        themePrefs.setSoundEnabled(false)
        assertFalse(themePrefs.isSoundEnabled.value)
        
        // Toggle sound back on
        themePrefs.setSoundEnabled(true)
        assertTrue(themePrefs.isSoundEnabled.value)
    }
}
