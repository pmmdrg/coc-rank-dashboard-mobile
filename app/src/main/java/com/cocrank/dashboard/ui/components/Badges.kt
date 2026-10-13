package com.cocrank.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cocrank.dashboard.domain.model.RatingCategory

@Composable
fun RatingBadge(
    category: RatingCategory,
    modifier: Modifier = Modifier
) {
    val badgeColor = Color(category.colorHex)
    Box(
        modifier = modifier
            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, badgeColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = category.displayName,
            color = badgeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RankDiffBadge(
    diff: Int?,
    modifier: Modifier = Modifier
) {
    if (diff == null || diff == 0) {
        Text(
            text = "—",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = modifier
        )
        return
    }

    val isPositive = diff > 0
    val text = if (isPositive) "▲ $diff" else "▼ ${-diff}"
    val color = if (isPositive) Color(0xFF10B981) else Color(0xFFE11D48)

    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
