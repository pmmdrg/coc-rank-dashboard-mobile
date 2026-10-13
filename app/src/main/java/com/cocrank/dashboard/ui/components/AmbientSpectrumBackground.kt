package com.cocrank.dashboard.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Nền chuyển động nhiều khối màu chậm và thay đổi liên tục như dải màu RGB (Aurora / Ambient Mesh)
 * - Sử dụng Canvas tăng tốc GPU với các mảng radial gradient mềm mại
 * - Chu kỳ lượn sóng êm dịu, không giật cục, tiết kiệm pin tối đa
 * - Chiếu sáng xuyên qua các thẻ Liquid Glass bán trong suốt
 */
@Composable
fun AmbientSpectrumBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_spectrum_loop")

    // Pha chuyển động chậm 1: 34 giây
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 34000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    // Pha chuyển động chậm 2: 48 giây
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    // Pha chuyển sắc màu RGB: 60 giây
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "color_shift"
    )

    val baseBg = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = baseBg)

            val w = size.width
            val h = size.height

            // Độ đậm màu nhẹ nhàng, tạo chiều sâu dưới các thẻ liquid glass
            val alphaFactor = if (isDark) 0.17f else 0.11f

            // Khối 1: Emerald Cyan / Mint (Góc trên uốn lượn)
            val c1X = w * (0.35f + 0.25f * cos(phase1))
            val c1Y = h * (0.22f + 0.12f * sin(phase1))
            val rad1 = w * 0.85f
            val col1 = Color(
                red = (0.05f + 0.05f * sin(colorShift)).coerceIn(0f, 1f),
                green = (0.75f + 0.20f * cos(colorShift)).coerceIn(0f, 1f),
                blue = (0.65f + 0.25f * sin(colorShift)).coerceIn(0f, 1f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(col1.copy(alpha = alphaFactor * 1.25f), Color.Transparent),
                    center = Offset(c1X, c1Y),
                    radius = rad1
                ),
                radius = rad1,
                center = Offset(c1X, c1Y)
            )

            // Khối 2: Royal Sapphire / Deep Indigo (Khu vực giữa & phải)
            val c2X = w * (0.70f + 0.22f * sin(phase2))
            val c2Y = h * (0.48f + 0.18f * cos(phase2))
            val rad2 = w * 0.95f
            val col2 = Color(
                red = (0.25f + 0.15f * cos(colorShift)).coerceIn(0f, 1f),
                green = (0.20f + 0.15f * sin(colorShift)).coerceIn(0f, 1f),
                blue = (0.88f + 0.10f * cos(colorShift)).coerceIn(0f, 1f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(col2.copy(alpha = alphaFactor), Color.Transparent),
                    center = Offset(c2X, c2Y),
                    radius = rad2
                ),
                radius = rad2,
                center = Offset(c2X, c2Y)
            )

            // Khối 3: Neon Purple / Violet / Magenta (Khu vực trung tâm dưới)
            val c3X = w * (0.25f + 0.20f * sin(phase1 * 0.7f))
            val c3Y = h * (0.72f + 0.15f * cos(phase2 * 0.8f))
            val rad3 = w * 0.90f
            val col3 = Color(
                red = (0.65f + 0.25f * sin(colorShift + 1.5f)).coerceIn(0f, 1f),
                green = (0.15f + 0.10f * cos(colorShift)).coerceIn(0f, 1f),
                blue = (0.80f + 0.18f * sin(colorShift)).coerceIn(0f, 1f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(col3.copy(alpha = alphaFactor * 1.15f), Color.Transparent),
                    center = Offset(c3X, c3Y),
                    radius = rad3
                ),
                radius = rad3,
                center = Offset(c3X, c3Y)
            )

            // Khối 4: Warm Gold / Amber / Radiant Bronze (Điểm xuyết thương hiệu CoC)
            val c4X = w * (0.60f + 0.25f * cos(phase2 * 0.9f))
            val c4Y = h * (0.85f + 0.10f * sin(phase1 * 1.1f))
            val rad4 = w * 0.75f
            val col4 = Color(
                red = (0.85f + 0.12f * cos(colorShift + 2.0f)).coerceIn(0f, 1f),
                green = (0.65f + 0.15f * sin(colorShift + 1.0f)).coerceIn(0f, 1f),
                blue = (0.15f + 0.10f * cos(colorShift)).coerceIn(0f, 1f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(col4.copy(alpha = alphaFactor * 0.85f), Color.Transparent),
                    center = Offset(c4X, c4Y),
                    radius = rad4
                ),
                radius = rad4,
                center = Offset(c4X, c4Y)
            )
        }

        content()
    }
}
