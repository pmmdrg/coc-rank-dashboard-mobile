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

    val borderBrush = if (accentBorder != null) {
        Brush.verticalGradient(
            listOf(
                accentBorder.copy(alpha = 0.85f),
                accentBorder.copy(alpha = 0.35f)
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

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = spotColor,
            ambientColor = ambientColor
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                listOf(
                    baseSurface.copy(alpha = surfaceAlphaStart),
                    baseSurface.copy(alpha = surfaceAlphaEnd)
                )
            )
        )
        .border(
            width = 1.dp,
            brush = borderBrush,
            shape = shape
        )
}
