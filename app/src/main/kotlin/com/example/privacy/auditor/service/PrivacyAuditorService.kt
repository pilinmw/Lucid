package com.example.privacy.auditor.service

import android.accessibilityservice.AccessibilityService
import android.app.AppOpsManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.ComposeView
import com.example.privacy.auditor.haptics.HapticEngine
import com.example.privacy.auditor.model.PrivacyThreatLevel
import com.example.privacy.auditor.ui.FluidEnergyBall
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class PrivacyAuditorService : AccessibilityService() {

    private val threatState = MutableStateFlow(PrivacyThreatLevel.NONE)
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: ComposeView
    private lateinit var hapticEngine: HapticEngine
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        hapticEngine = HapticEngine(this)
        setupOverlay()
        startTelemetry()
    }

    private fun setupOverlay() {
        overlayView = ComposeView(this).apply {
            setContent {
                val currentThreat by threatState.collectAsState()
                FluidEnergyBall(threatLevel = currentThreat)
            }
        }

        val params = WindowManager.LayoutParams(
            300, 300, // 约束包围盒最小化
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 50
            y = 100
        }

        windowManager.addView(overlayView, params)
    }

    private fun startTelemetry() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        
        // 监听传感器活跃状态变化 (实现文档中 AppOps 遥测逻辑)
        // 注意：在标准 Android 11+ 中，需要特定权限或 Root/ADB 授权来监听其他应用的 OpActive
        val listener = object : AppOpsManager.OnOpActiveChangedListener {
            override fun onOpActiveChanged(op: String, uid: Int, packageName: String, active: Boolean) {
                scope.launch {
                    val newThreat = when (op) {
                        AppOpsManager.OPSTR_CAMERA -> if (active) PrivacyThreatLevel.CAMERA else PrivacyThreatLevel.NONE
                        AppOpsManager.OPSTR_RECORD_AUDIO -> if (active) PrivacyThreatLevel.MICROPHONE else PrivacyThreatLevel.NONE
                        AppOpsManager.OPSTR_FINE_LOCATION -> if (active) PrivacyThreatLevel.LOCATION else PrivacyThreatLevel.NONE
                        else -> threatState.value
                    }
                    
                    if (newThreat != threatState.value) {
                        threatState.value = newThreat
                        triggerFeedback(newThreat)
                    }
                }
            }
        }

        val opsToWatch = arrayOf(
            AppOpsManager.OPSTR_CAMERA,
            AppOpsManager.OPSTR_RECORD_AUDIO,
            AppOpsManager.OPSTR_FINE_LOCATION
        )
        
        // 这里简化了监听注册，实际可能需要循环注册每个 OP
        opsToWatch.forEach { op ->
            try { appOps.startWatchingActive(arrayOf(op), mainExecutor, listener) } catch (e: Exception) {}
        }
    }

    private fun triggerFeedback(level: PrivacyThreatLevel) {
        when (level) {
            PrivacyThreatLevel.CAMERA -> hapticEngine.pulseConfirm()
            PrivacyThreatLevel.MICROPHONE -> hapticEngine.pulseDanger()
            PrivacyThreatLevel.LOCATION -> hapticEngine.pulseHeartbeat()
            PrivacyThreatLevel.STORAGE -> hapticEngine.pulseWarning()
            else -> {}
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 利用无障碍服务监听窗口变化，实现归因探测
        // 启发式检测：如果前台没有相册相关的 UI，但后台有存储权限的 App 活跃，触发深紫色预警
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // 这里可以解析 UI 节点，判定是否存在“权限搭便车”现象
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) windowManager.removeView(overlayView)
        scope.cancel()
    }
}
