package com.cocrank.dashboard.ui.screens.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cocrank.dashboard.R
import com.cocrank.dashboard.domain.model.RankChangeItem
import com.cocrank.dashboard.ui.components.RankDiffBadge
import com.cocrank.dashboard.ui.components.liquidGlass
import com.cocrank.dashboard.ui.screens.dashboard.DashboardViewModel
import com.cocrank.dashboard.ui.theme.GoldAccent
import kotlin.math.abs

private enum class ChangesFilter {
    ALL, ROSE, FELL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankChangesScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val allChanges = state.rankChanges?.changes ?: emptyList()

    val cleanMyTag = state.savedPlayerTag.trim().uppercase().removePrefix("#")

    // 1. Biến động riêng của bản thân: hiển thị dù không có hoạt động thi đấu hoặc bị tụt hạng thụ động
    val myChange = remember(allChanges, cleanMyTag) {
        allChanges.firstOrNull {
            it.isMe ||
            (cleanMyTag.isNotBlank() && (
                it.id.trim().uppercase().removePrefix("#") == cleanMyTag ||
                it.playerTag?.trim()?.uppercase()?.removePrefix("#") == cleanMyTag
            ))
        }
    }

    // 2. Chỉ tính các biến động có thay đổi về cúp hoặc lượt công/thủ (loại trừ bị đẩy thụ động)
    val activeChanges = remember(allChanges) {
        allChanges.filter { c ->
            c.rankDiff != 0 && (c.cupsDiff != 0 || (c.attacksDiff ?: 0) != 0 || (c.defensesDiff ?: 0) != 0)
        }
    }

    val roseChanges = remember(activeChanges) { activeChanges.filter { it.rankDiff > 0 } }
    val fellChanges = remember(activeChanges) { activeChanges.filter { it.rankDiff < 0 } }

    var selectedFilter by remember { mutableStateOf(ChangesFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(activeChanges, selectedFilter, searchQuery) {
        val base = when (selectedFilter) {
            ChangesFilter.ALL -> activeChanges
            ChangesFilter.ROSE -> roseChanges
            ChangesFilter.FELL -> fellChanges
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            val q = searchQuery.trim().lowercase()
            base.filter {
                it.name.lowercase().contains(q) ||
                it.playerTag?.lowercase()?.contains(q) == true ||
                it.newRank.toString() == q ||
                it.oldRank.toString() == q
            }
        }
    }

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
                            text = if (state.rankChanges?.updatedAt.isNullOrBlank())
                                "So với lần fetch trước"
                            else
                                "Cập nhật lúc ${state.rankChanges?.updatedAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncData() },
                        enabled = !state.isSyncing
                    ) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = GoldAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ph_arrows_clockwise),
                                contentDescription = "Đồng bộ",
                                tint = GoldAccent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. THẺ NHẮC NHỞ BIẾN ĐỘNG CỦA BẢN THÂN (HERO REMINDER CARD)
            if (myChange != null) {
                item(key = "my_reminder_card") {
                    MyRankReminderCard(
                        item = myChange,
                        myName = state.season?.players?.firstOrNull { it.isMe }?.name ?: "Bạn"
                    )
                }
            }

            // 2. THẺ TỔNG QUAN & BỘ LỌC BIẾN ĐỘNG (Liquid Glass Card)
            item(key = "filters_card") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(shape = RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BIẾN ĐỘNG BẢNG ĐẤU",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${activeChanges.size} người thay đổi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldAccent
                            )
                        }

                        // Segmented Filter Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                label = "Tất cả (${activeChanges.size})",
                                isSelected = selectedFilter == ChangesFilter.ALL,
                                onClick = { selectedFilter = ChangesFilter.ALL },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                label = "Tăng (${roseChanges.size})",
                                isSelected = selectedFilter == ChangesFilter.ROSE,
                                indicatorColor = Color(0xFF10B981),
                                onClick = { selectedFilter = ChangesFilter.ROSE },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                label = "Tụt (${fellChanges.size})",
                                isSelected = selectedFilter == ChangesFilter.FELL,
                                indicatorColor = Color(0xFFE11D48),
                                onClick = { selectedFilter = ChangesFilter.FELL },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Search Input
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Tìm theo tên, thứ hạng...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ph_magnifying_glass),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                focusedBorderColor = GoldAccent.copy(alpha = 0.8f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }

            // 3. DANH SÁCH CÁC BIẾN ĐỘNG THỨ HẠNG HOẠT ĐỘNG
            if (activeChanges.isEmpty()) {
                item(key = "empty_active_changes") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(shape = RoundedCornerShape(18.dp))
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ph_chart_line_up),
                                contentDescription = null,
                                tint = GoldAccent.copy(alpha = 0.7f),
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Chưa có biến động thi đấu mới",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hệ thống chỉ thống kê các đối thủ có thi đấu thay đổi cúp hoặc lượt công/thủ (loại trừ thay đổi thụ động). Nhấn nút đồng bộ để kiểm tra cập nhật mới nhất.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.syncData() },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Đồng Bộ Dữ Liệu Ngay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else if (filteredList.isEmpty()) {
                item(key = "no_search_results") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(shape = RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không tìm thấy biến động nào phù hợp",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    ActiveRankChangeCard(item = item)
                }
            }
        }
    }
}

@Composable
private fun MyRankReminderCard(
    item: RankChangeItem,
    myName: String
) {
    val rankDiff = item.rankDiff
    val cupsDiff = item.cupsDiff

    val (cardBorder, bgTint, titleText, descText) = when {
        rankDiff > 0 -> {
            Quad(
                Color(0xFF10B981),
                Color(0xFF10B981).copy(alpha = 0.12f),
                "🎉 Bạn đã tăng $rankDiff bậc",
                "Phong độ xuất sắc! Bạn đã bứt phá thứ hạng thành công so với lần đồng bộ trước."
            )
        }
        rankDiff < 0 -> {
            Quad(
                Color(0xFFE11D48),
                Color(0xFFE11D48).copy(alpha = 0.12f),
                "⚠️ Bạn đã bị tụt ${abs(rankDiff)} bậc",
                "Thứ hạng tạm thời giảm sút. Hãy chuẩn bị và hoàn thành các lượt đánh còn lại!"
            )
        }
        else -> {
            Quad(
                Color(0xFF0284C7),
                Color(0xFF0284C7).copy(alpha = 0.12f),
                "🛡️ Thứ hạng của bạn được giữ nguyên",
                "Vị trí của bạn ổn định. Tiếp tục tích lũy cúp để bứt phá lên nhóm trên!"
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(20.dp),
                elevation = 14.dp,
                accentBorder = cardBorder
            )
            .background(bgTint)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(cardBorder.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BẢN THÂN",
                            color = cardBorder,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = titleText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Rank Change Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardBorder.copy(alpha = 0.18f))
                        .border(1.dp, cardBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (item.oldRank != item.newRank) "#${item.oldRank} ➔ #${item.newRank}" else "#${item.newRank}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = cardBorder
                    )
                }
            }

            Text(
                text = descText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            // Dãy số liệu biến động (Pills)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (cupsDiff != 0) {
                    StatDiffPill(
                        text = if (cupsDiff > 0) "+$cupsDiff 🏆" else "$cupsDiff 🏆",
                        color = if (cupsDiff > 0) Color(0xFF10B981) else Color(0xFFE11D48),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (item.attacksDiff != 0) {
                    StatDiffPill(
                        text = if (item.attacksDiff > 0) "+${item.attacksDiff} công" else "${item.attacksDiff} công",
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (item.defensesDiff != 0) {
                    StatDiffPill(
                        text = if (item.defensesDiff > 0) "+${item.defensesDiff} thủ" else "${item.defensesDiff} thủ",
                        color = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveRankChangeCard(item: RankChangeItem) {
    val isClimb = item.rankDiff > 0
    val cupsDiff = item.cupsDiff

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!item.playerTag.isNullOrBlank()) {
                        Text(
                            text = item.playerTag,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                RankDiffBadge(diff = item.rankDiff)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Thứ hạng:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "#${item.oldRank} ➔ #${item.newRank}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isClimb) Color(0xFF10B981) else Color(0xFFE11D48)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (cupsDiff != 0) {
                        val cupText = if (cupsDiff > 0) "+$cupsDiff 🏆" else "$cupsDiff 🏆"
                        val cupColor = if (cupsDiff > 0) Color(0xFF10B981) else Color(0xFFE11D48)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(cupColor.copy(alpha = 0.15f))
                                .border(0.5.dp, cupColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = cupText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cupColor)
                        }
                    }

                    if (item.attacksDiff > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .border(0.5.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "+${item.attacksDiff}⚔️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }

                    if (item.defensesDiff > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                                .border(0.5.dp, Color(0xFF0284C7).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "+${item.defensesDiff}🛡️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatDiffPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .border(0.5.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    indicatorColor: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) GoldAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = if (isSelected) GoldAccent.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (indicatorColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
