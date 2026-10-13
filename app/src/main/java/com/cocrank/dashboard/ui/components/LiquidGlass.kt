package com.cocrank.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hiệu ứng Liquid Glass (Kính lỏng) cao cấp đồng bộ với Navigation Bar
 * - Viền specular rim gradient phản quang ánh sáng
 * - Nền mờ kính bán trong suốt (translucent) đón nhận các khối màu RGB bên dưới
 * - Đổ bóng ambient & spot tạo chiều sâu nổi khối
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 10.dp,
    accentBorder: Color? = null,
    surfaceAlphaStart: Float = 0.72f,
    surfaceAlphaEnd: Float = 0.84f,
    spotColor: Color = Color.Black.copy(alpha = 0.45f),
    ambientColor: Color = Color.Black.copy(alpha = 0.25f)
): Modifier {
    val baseSurface = MaterialTheme.colorScheme.surface
    val baseOutline = MaterialTheme.colorScheme.outline
    val isLight = baseSurface.luminance() > 0.5f

    val effectiveElevation = if (isLight) (elevation * 0.5f).coerceAtLeast(2.dp) else elevation
    val effectiveSpotColor = if (isLight) Color(0x140F172A) else spotColor
    val effectiveAmbientColor = if (isLight) Color(0x0C0F172A) else ambientColor

    val borderBrush = if (accentBorder != null) {
        Brush.verticalGradient(
            listOf(
                accentBorder.copy(alpha = 0.85f),
                accentBorder.copy(alpha = 0.35f)
            )
        )
    } else if (isLight) {
        Brush.verticalGradient(
            listOf(
                baseOutline.copy(alpha = 0.70f),
                baseOutline.copy(alpha = 0.40f),
                baseOutline.copy(alpha = 0.20f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.28f),
                baseOutline.copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    }

    val startAlpha = if (isLight) 0.88f else surfaceAlphaStart
    val endAlpha = if (isLight) 0.94f else surfaceAlphaEnd

    return this
        .shadow(
            elevation = effectiveElevation,
            shape = shape,
            spotColor = effectiveSpotColor,
            ambientColor = effectiveAmbientColor
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                listOf(
                    baseSurface.copy(alpha = startAlpha),
                    baseSurface.copy(alpha = endAlpha)
                )
            )
        )
        .border(
            width = 1.dp,
            brush = borderBrush,
            shape = shape
        )
}
