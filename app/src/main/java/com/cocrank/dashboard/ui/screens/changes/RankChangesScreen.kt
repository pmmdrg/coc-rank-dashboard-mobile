package com.cocrank.dashboard.ui.screens.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cocrank.dashboard.R
import com.cocrank.dashboard.domain.model.RankChangeItem
import com.cocrank.dashboard.ui.components.RankDiffBadge
import com.cocrank.dashboard.ui.screens.dashboard.DashboardViewModel
import com.cocrank.dashboard.ui.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankChangesScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val changes = state.rankChanges?.changes ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Biến Động Thứ Hạng",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (state.rankChanges?.updatedAt.isNullOrBlank()) "Chưa có lần đồng bộ thứ hai" else "Cập nhật lúc: ${state.rankChanges?.updatedAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        if (changes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ph_chart_line_up),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Chưa có biến động thứ hạng",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Biến động sẽ được ghi nhận sau khi bạn thực hiện đồng bộ lại dữ liệu CoC API hoặc có thay đổi số cúp của người chơi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(changes, key = { it.id }) { item ->
                    RankChangeCard(item)
                }
            }
        }
    }
}

@Composable
fun RankChangeCard(item: RankChangeItem) {
    val isMe = item.isMe
    val cupsDiff = item.cupsDiff

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isMe) 1.5.dp else 1.dp,
                color = if (isMe) GoldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isMe) GoldAccent else MaterialTheme.colorScheme.onSurface
                    )
                    if (isMe) {
                        Box(
                            modifier = Modifier
                                .background(GoldAccent.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
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
                RankDiffBadge(diff = item.rankDiff)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Thứ hạng:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "#${item.oldRank} ➔ #${item.newRank}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Biến động cúp:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val cupText = if (cupsDiff > 0) "+$cupsDiff 🏆" else if (cupsDiff < 0) "$cupsDiff 🏆" else "0 🏆"
                    val cupColor = if (cupsDiff > 0) Color(0xFF10B981) else if (cupsDiff < 0) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant
                    Text(
                        text = "${item.newCups} ($cupText)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = cupColor
                    )
                }
            }
        }
    }
}
