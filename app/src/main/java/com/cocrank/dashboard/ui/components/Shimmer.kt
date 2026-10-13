package com.cocrank.dashboard.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cocrank.dashboard.ui.theme.GoldAccent

/**
 * Brush hiệu ứng Shimmer quét sóng ánh sáng liên tục
 */
@Composable
fun shimmerBrush(
    targetValue: Float = 1300f,
    durationMillis: Int = 1300
): Brush {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    val shimmerColors = if (isLight) {
        listOf(
            Color(0xFFCBD5E1).copy(alpha = 0.35f),
            Color(0xFFFFFFFF).copy(alpha = 0.85f),
            Color(0xFFCBD5E1).copy(alpha = 0.35f)
        )
    } else {
        listOf(
            Color(0xFF1E2337).copy(alpha = 0.55f),
            Color(0xFF3B4868).copy(alpha = 0.90f),
            Color(0xFF1E2337).copy(alpha = 0.55f)
        )
    }

    val transition = rememberInfiniteTransition(label = "shimmer_infinite_transition")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate_anim"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation, y = translateAnimation)
    )
}

/**
 * Modifier phủ lớp Shimmer lên một Composable khi isLoading = true
 */
@Composable
fun Modifier.shimmer(
    isLoading: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier {
    return if (isLoading) {
        val brush = shimmerBrush()
        this
            .clip(shape)
            .background(brush)
    } else {
        this
    }
}

/**
 * Banner thông báo đang fetch Supercell API với hiệu ứng Shimmer nổi bật
 */
@Composable
fun ShimmerSyncBanner(
    isSyncing: Boolean,
    syncMessage: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isSyncing,
        enter = fadeIn(tween(250)) + expandVertically(tween(300)),
        exit = fadeOut(tween(250)) + shrinkVertically(tween(300)),
        modifier = modifier
    ) {
        val brush = shimmerBrush(durationMillis = 1100)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(14.dp), elevation = 6.dp)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GoldAccent.copy(alpha = 0.18f))
                            .border(1.dp, GoldAccent.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = GoldAccent,
                            strokeWidth = 2.dp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = syncMessage?.ifBlank { "Đang đồng bộ dữ liệu Supercell API..." } ?: "Đang đồng bộ dữ liệu Supercell API...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Đang cập nhật thứ hạng, cúp và lịch sử lượt đánh mới nhất...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                // Dải Shimmer line bên dưới
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(brush)
                )
            }
        }
    }
}

/**
 * Skeleton Card mô phỏng thẻ người chơi khi đang tải
 */
@Composable
fun PlayerItemSkeletonCard(
    modifier: Modifier = Modifier
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(14.dp), elevation = 4.dp)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank Circle Skeleton
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(brush)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Name & Tag Skeletons
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }

                // Cups Skeleton
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
            }

            // Stats row Skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
            }
        }
    }
}

/**
 * Skeleton Card mô phỏng Tổng quan mùa giải khi đang tải
 */
@Composable
fun SeasonOverviewSkeletonCard(
    modifier: Modifier = Modifier
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(18.dp), elevation = 6.dp)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header title skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
            }

            // League & dates skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
            }

            // Stat pills skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush)
                )
            }
        }
    }
}
