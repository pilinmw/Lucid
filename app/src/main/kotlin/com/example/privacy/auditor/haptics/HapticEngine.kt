package com.example.privacy.auditor.haptics

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build

class HapticEngine(context: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * 触发心跳脉冲：低频/环境感知 (位置更新)
     */
    fun pulseHeartbeat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            vibrator.vibrate(effect)
        }
    }

    /**
     * 触发清脆双击：中度/显式授权 (开关确认)
     */
    fun pulseConfirm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            vibrator.vibrate(effect)
        }
    }

    /**
     * 触发摇摆颤振：高危/异常穿透 (相册扫描)
     */
    fun pulseWarning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val composition = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SPIN, 0.8f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 100)
                .compose()
            vibrator.vibrate(composition)
        }
    }

    /**
     * 触发紧急警报：严重/恶意监听 (录音)
     */
    fun pulseDanger() {
        // 模拟不断升级的风险信号
        val timings = longArrayOf(0, 50, 50, 100, 50, 150)
        val amplitudes = intArrayOf(0, 100, 0, 180, 0, 255)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        }
    }
}
