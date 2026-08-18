package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.SoundEffectConstants
import android.view.View
import com.example.R

class SoundManager private constructor(context: Context) {

    private val applicationContext = context.applicationContext
    private var soundPool: SoundPool? = null
    private var checkSoundId: Int = 0
    private var uncheckSoundId: Int = 0
    private var completeAllSoundId: Int = 0
    private var processSoundId: Int = 0
    private var rewardSoundId: Int = 0
    private var isLoaded: Boolean = false

    init {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool?.setOnLoadCompleteListener { _, _, status ->
                if (status == 0) {
                    isLoaded = true
                }
            }

            checkSoundId = soundPool?.load(applicationContext, R.raw.task_check, 1) ?: 0
            uncheckSoundId = soundPool?.load(applicationContext, R.raw.task_uncheck, 1) ?: 0
            completeAllSoundId = soundPool?.load(applicationContext, R.raw.task_complete_all, 1) ?: 0
            processSoundId = soundPool?.load(applicationContext, R.raw.process, 1) ?: 0
            rewardSoundId = soundPool?.load(applicationContext, R.raw.reward, 1) ?: 0
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "Error initializing SoundPool", e)
        }
    }

    fun playTaskCheck(enabled: Boolean = true, fallbackView: View? = null) {
        if (!enabled) return
        if (isLoaded && checkSoundId != 0) {
            soundPool?.play(checkSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } else {
            fallbackView?.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    fun playTaskUncheck(enabled: Boolean = true, fallbackView: View? = null) {
        if (!enabled) return
        if (isLoaded && uncheckSoundId != 0) {
            soundPool?.play(uncheckSoundId, 0.7f, 0.7f, 1, 0, 0.95f)
        } else {
            fallbackView?.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    fun playTaskCompleteAll(enabled: Boolean = true, fallbackView: View? = null) {
        if (!enabled) return
        if (isLoaded && completeAllSoundId != 0) {
            soundPool?.play(completeAllSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } else {
            fallbackView?.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    fun playReward(enabled: Boolean = true, fallbackView: View? = null) {
        if (!enabled) return
        if (isLoaded && rewardSoundId != 0) {
            soundPool?.play(rewardSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } else {
            fallbackView?.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    fun playLevelUp(enabled: Boolean = true, fallbackView: View? = null) {
        playReward(enabled, fallbackView)
    }

    private var activeProcessStreamId: Int = 0

    fun startProcessSound(enabled: Boolean = true, fallbackView: View? = null): Int {
        if (!enabled) return 0
        stopProcessSound()
        activeProcessStreamId = if (isLoaded && processSoundId != 0) {
            soundPool?.play(processSoundId, 1.0f, 1.0f, 1, 0, 1.0f) ?: 0
        } else {
            fallbackView?.playSoundEffect(SoundEffectConstants.CLICK)
            0
        }
        return activeProcessStreamId
    }

    fun stopProcessSound() {
        if (activeProcessStreamId != 0) {
            try {
                soundPool?.stop(activeProcessStreamId)
            } catch (_: Exception) {}
            activeProcessStreamId = 0
        }
    }

    fun playProcess(enabled: Boolean = true, fallbackView: View? = null) {
        startProcessSound(enabled, fallbackView)
    }

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            isLoaded = false
            INSTANCE = null
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "Error releasing SoundPool", e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SoundManager? = null

        fun getInstance(context: Context): SoundManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SoundManager(context).also { INSTANCE = it }
            }
        }
    }
}
