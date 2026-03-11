package com.example.privacy.auditor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import com.example.privacy.auditor.model.PrivacyThreatLevel

// 元球流体着色器 (AGSL)
private const val METABALL_SHADER = """
    uniform float2 u_resolution;
    uniform float u_time;
    uniform float4 u_color;
    uniform float u_intensity;

    float metaball(float2 p, float2 center, float radius) {
        return radius / length(p - center);
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / u_resolution.xy;
        float2 p = uv * 2.0 - 1.0;
        p.x *= u_resolution.x / u_resolution.y;

        float r = 0.0;
        // 动态生成两个融合的元球
        float2 c1 = float2(sin(u_time * 1.2) * 0.2, cos(u_time * 0.8) * 0.2);
        float2 c2 = float2(cos(u_time * 1.5) * 0.15, sin(u_time * 1.1) * 0.15);
        
        r += metaball(p, c1, 0.15);
        r += metaball(p, c2, 0.12);

        // 阈值截断产生 Gooey Effect
        if (r > 1.0) {
            float alpha = smoothstep(1.0, 1.1, r);
            return u_color * alpha * u_intensity;
        }
        return half4(0.0);
    }
"""

@Composable
fun FluidEnergyBall(
    threatLevel: PrivacyThreatLevel,
    modifier: Modifier = Modifier
) {
    if (threatLevel == PrivacyThreatLevel.NONE) return

    val color = when (threatLevel) {
        PrivacyThreatLevel.LOCATION -> android.graphics.Color.valueOf(0f, 0.7f, 1f) // 天青色
        PrivacyThreatLevel.MICROPHONE -> android.graphics.Color.valueOf(1f, 0.75f, 0f) // 琥珀色
        PrivacyThreatLevel.CAMERA -> android.graphics.Color.valueOf(1f, 0.1f, 0.1f) // 猩红色
        PrivacyThreatLevel.STORAGE -> android.graphics.Color.valueOf(0.5f, 0f, 0.5f) // 深紫色
        else -> android.graphics.Color.valueOf(0f, 0f, 0f, 0f)
    }

    val shader = remember { RuntimeShader(METABALL_SHADER) }
    var time by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time = it / 1_000_000_000f }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // 应用毛玻璃高斯模糊
                renderEffect = RenderEffect.createBlurEffect(
                    30f, 30f, android.graphics.Shader.TileMode.DECAL
                ).asComposeRenderEffect()
            }
    ) {
        shader.setFloatUniform("u_resolution", size.width, size.height)
        shader.setFloatUniform("u_time", time)
        shader.setFloatUniform("u_color", color.red(), color.green(), color.blue(), 1f)
        shader.setFloatUniform("u_intensity", if (threatLevel == PrivacyThreatLevel.CAMERA) 1.5f else 1.0f)
        
        drawContext.canvas.nativeCanvas.drawPaint(android.graphics.Paint().apply {
            this.shader = shader
        })
    }
}
