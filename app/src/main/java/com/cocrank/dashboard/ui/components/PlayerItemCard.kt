package com.cocrank.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cocrank.dashboard.domain.model.Player
import com.cocrank.dashboard.ui.theme.GoldAccent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerItemCard(
    player: Player,
    maxAttacks: Int = 24,
    maxDefenses: Int = 24,
    modifier: Modifier = Modifier
) {
    val isMe = player.isMe
    val rankBadgeColor = when (player.rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    }

    val cardBorderColor = if (isMe) {
        GoldAccent
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    }

    val cardBackground = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackground)
            .border(
                width = if (isMe) 1.5.dp else 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Rank Circle
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(rankBadgeColor.copy(alpha = 0.2f))
                        .border(1.dp, rankBadgeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${player.rank}",
                        color = if (player.rank <= 3) rankBadgeColor else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name, Tags & Matchup Badges
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isMe) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isMe) GoldAccent else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isMe) {
                            Box(
                                modifier = Modifier
                                    .background(GoldAccent.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "BẠN",
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!player.clanName.isNullOrBlank()) {
                            Text(
                                text = player.clanName,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(text = "•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        if (!player.playerTag.isNullOrBlank()) {
                            Text(
                                text = player.playerTag,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Badges Đối Đầu từ Battle Log (So với bản thân)
                    if (!isMe && (player.attackedByMe.isNotEmpty() || player.defendedAgainstMe.isNotEmpty())) {
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (player.attackedByMe.isNotEmpty()) {
                                val atkSummary = player.attackedByMe.joinToString(", ") { log ->
                                    val dest = if (log.destructionPercentage > 0) " (${log.destructionPercentage.toInt()}%)" else ""
                                    "${log.stars}★$dest"
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                        .border(0.5.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "⚔️ Bạn công: $atkSummary",
                                        color = Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (player.defendedAgainstMe.isNotEmpty()) {
                                val defSummary = player.defendedAgainstMe.joinToString(", ") { log ->
                                    val dest = if (log.destructionPercentage > 0) " (${log.destructionPercentage.toInt()}%)" else ""
                                    "${log.stars}★$dest"
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                                        .border(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "🛡️ Bị đánh: $defSummary",
                                        color = Color(0xFFF59E0B),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Rank Change & Rating
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RatingBadge(category = player.rating)
                    RankDiffBadge(diff = player.rankDiff)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cups and Max Cups Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "CÚP HIỆN TẠI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${player.currentCups} 🏆",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "CÚP TỐI ĐA DỰ KIẾN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${player.maxPossibleCups} 🏆",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Attack & Defense Progress bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Attacks
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Công: ${player.attacks}/$maxAttacks",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { (player.attacks.toFloat() / maxAttacks.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF10B981),
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    )
                }

                // Defenses
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Thủ: ${player.defenses}/$maxDefenses",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { (player.defenses.toFloat() / maxDefenses.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF0284C7),
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}
