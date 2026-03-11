package com.example.privacy.auditor.model

enum class PrivacyThreatLevel {
    NONE,       // 隐形状态
    LOCATION,   // 天青色：地理位置 (Ping)
    MICROPHONE, // 琥珀色：麦克风 (录音)
    CAMERA,     // 猩红色：摄像头 (开启)
    STORAGE     // 深紫色：隐蔽相册扫描
}
