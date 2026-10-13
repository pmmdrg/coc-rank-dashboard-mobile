package com.cocrank.dashboard.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cocrank.dashboard.R
import com.cocrank.dashboard.data.LeagueIcons
import com.cocrank.dashboard.domain.model.RatingCategory
import com.cocrank.dashboard.ui.components.PlayerItemCard
import com.cocrank.dashboard.ui.components.SearchAndFilterBar
import com.cocrank.dashboard.ui.components.SyncPromptDialog
import com.cocrank.dashboard.ui.theme.GoldAccent
import com.cocrank.dashboard.ui.theme.GreenSuperior
import com.cocrank.dashboard.ui.theme.PurpleDominant
import com.cocrank.dashboard.ui.theme.RedAlarm
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val season = state.season
    val stats = state.stats
    val context = LocalContext.current

    var isOverviewCollapsed by remember { mutableStateOf(false) }
    var isHighlightsCollapsed by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrorMessage()
        }
    }

    if (state.showConfigDialog) {
        SyncPromptDialog(
            initialPlayerTag = state.savedPlayerTag,
            onDismiss = { viewModel.dismissConfigDialog() },
            onConfirm = { tag -> viewModel.savePlayerTag(tag) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Thông Tin Mùa Giải",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PurpleDominant.copy(alpha = 0.2f))
                                    .border(1.dp, PurpleDominant.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = season?.league ?: "Legend",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleDominant
                                )
                            }
                        }
                        Text(
                            text = if (state.isSyncing) (state.syncMessage ?: "Đang đồng bộ...") else "Mùa giải: ${season?.seasonName ?: "Hiện tại"}",
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
                                contentDescription = "Đồng bộ API",
                                tint = GoldAccent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // PHẦN THÔNG TIN MÙA GIẢI & BỘ CHỌN MÙA (Đồng bộ SeasonMetaForm trên Web)
            item {
                val leagueIconUrl = LeagueIcons.getLeagueIconUrl(season?.league, season?.leagueIconUrl)
                val allSeasons = state.allSeasons
                val activeIndex = state.activeSeasonIndex

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Bộ chọn Mùa giải (Current Season / Previous Season)
                    if (allSeasons.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allSeasons.forEachIndexed { idx, _ ->
                                val isSelected = activeIndex == idx
                                val label = if (idx == 0) "Mùa giải hiện tại" else "Mùa giải trước đó"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) GoldAccent.copy(alpha = 0.18f) else Color.Transparent)
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) GoldAccent.copy(alpha = 0.5f) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.selectSeasonIndex(idx) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Card Thông tin Giải đấu & Thời gian mùa giải
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Cột 1: Huy hiệu giải đấu & Tên bảng đấu
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GoldAccent.copy(alpha = 0.12f))
                                        .border(1.dp, GoldAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(leagueIconUrl)
                                            .crossfade(true)
                                            .error(R.drawable.ph_trophy)
                                            .placeholder(R.drawable.ph_trophy)
                                            .build(),
                                        contentDescription = season?.league,
                                        modifier = Modifier.size(34.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = season?.league ?: "--",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!season?.leagueGroupTag.isNullOrBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(GoldAccent.copy(alpha = 0.15f))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = season!!.leagueGroupTag!!,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldAccent
                                                )
                                            }
                                        }
                                    }
                                    val dateText = if (!season?.startsAt.isNullOrBlank() && !season?.endsAt.isNullOrBlank()) {
                                        "${season!!.startsAt} → ${season!!.endsAt}"
                                    } else season?.seasonName ?: "--"
                                    Text(
                                        text = dateText,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Cột 2: Thăng / Giáng
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Top ${season?.promotionCount ?: 2} Thăng",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(RedAlarm.copy(alpha = 0.15f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Top ${season?.demotionCount ?: 1} Giáng",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RedAlarm
                                        )
                                    }
                                }
                                Text(
                                    text = "${season?.maxAttacks ?: 24} Lượt Công / Thủ",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            // MỤC 1: TỔNG QUAN MÙA GIẢI (SeasonOverviewSection)
            item {
                val totalPlayers = season?.players?.size ?: 0
                val activePlayers = season?.players?.filter { it.attacks > 0 || it.defenses > 0 || it.currentCups > 0 } ?: emptyList()
                val maxAtk = season?.maxAttacks ?: 24
                val finishedCount = activePlayers.count { it.attacks >= maxAtk }
                val inProgressCount = activePlayers.count { it.attacks in 1 until maxAtk }
                val notStartedCount = activePlayers.count { it.attacks == 0 }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.75f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Tiêu đề & Nút Thu gọn / Mở rộng
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TỔNG QUAN MÙA GIẢI",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.5.sp
                            )

                            val rotationAngle by animateFloatAsState(
                                targetValue = if (isOverviewCollapsed) 180f else 0f,
                                animationSpec = tween(220),
                                label = "season_overview_caret"
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { isOverviewCollapsed = !isOverviewCollapsed }
                                    .padding(horizontal = 9.dp, vertical = 4.5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (isOverviewCollapsed) "Mở rộng" else "Thu gọn",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        painter = painterResource(id = R.drawable.ph_caret_up),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .graphicsLayer { rotationZ = rotationAngle }
                                    )
                                }
                            }
                        }

                        // Nội dung chi tiết Tổng quan mùa giải
                        AnimatedVisibility(
                            visible = !isOverviewCollapsed,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Thẻ Quy tắc mùa giải
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "QUY TẮC BẢNG ĐẤU",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = 0.3.sp
                                        )
                                        Text(
                                            text = "${season?.maxAttacks ?: 24} Lượt Công • ${season?.maxDefenses ?: 24} Lượt Thủ",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "Top ${season?.promotionCount ?: 2} Thăng",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(RedAlarm.copy(alpha = 0.15f))
                                                .border(1.dp, RedAlarm.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "Top ${season?.demotionCount ?: 1} Giáng",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RedAlarm
                                            )
                                        }
                                    }
                                }

                                // Khối 1: Phân bổ phong độ người chơi
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "PHÂN BỔ PHONG ĐỘ",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = 0.3.sp
                                        )
                                        Text(
                                            text = "Tổng $totalPlayers người",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val dominantCount = stats?.ratingCounts?.get(RatingCategory.DOMINANT) ?: 0
                                        val superiorCount = stats?.ratingCounts?.get(RatingCategory.SUPERIOR) ?: 0
                                        val potentialCount = stats?.ratingCounts?.get(RatingCategory.POTENTIAL) ?: 0
                                        val alarmCount = stats?.ratingCounts?.get(RatingCategory.ALARM) ?: 0

                                        RatingCountPill(label = "Áp đảo", count = dominantCount, color = PurpleDominant, modifier = Modifier.weight(1f))
                                        RatingCountPill(label = "Vượt trội", count = superiorCount, color = GreenSuperior, modifier = Modifier.weight(1f))
                                        RatingCountPill(label = "Tiềm năng", count = potentialCount, color = Color(0xFF0284C7), modifier = Modifier.weight(1f))
                                        RatingCountPill(label = "Báo động", count = alarmCount, color = RedAlarm, modifier = Modifier.weight(1f))
                                    }
                                }

                                // Khối 2: Trạng thái lượt thi đấu
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "TRẠNG THÁI LƯỢT THI ĐẤU",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 0.3.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StatusCountCard(
                                            title = "Đã xong",
                                            count = finishedCount,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatusCountCard(
                                            title = "Đang đấu",
                                            count = inProgressCount,
                                            color = Color(0xFFF59E0B),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatusCountCard(
                                            title = "Chưa đấu",
                                            count = notStartedCount,
                                            color = Color(0xFF64748B),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // MỤC 2: KỶ LỤC & THỐNG KÊ NỔI BẬT (HighlightStatsTable)
            item {
                val activePlayers = season?.players?.filter { it.attacks > 0 || it.defenses > 0 || it.currentCups > 0 } ?: emptyList()
                val myPlayer = activePlayers.firstOrNull { it.isMe }
                val otherPlayers = activePlayers.filter { !it.isMe }

                val currentCupsBetterCount = if (myPlayer != null && otherPlayers.isNotEmpty()) {
                    otherPlayers.count { myPlayer.currentCups > it.currentCups }
                } else 0
                val currentCupsPercentBetter = if (otherPlayers.isNotEmpty()) {
                    Math.round((currentCupsBetterCount.toDouble() / otherPlayers.size) * 1000) / 10.0
                } else 100.0

                val maxCupsBetterCount = if (myPlayer != null && otherPlayers.isNotEmpty()) {
                    otherPlayers.count { myPlayer.maxPossibleCups > it.maxPossibleCups }
                } else 0
                val maxCupsPercentBetter = if (otherPlayers.isNotEmpty()) {
                    Math.round((maxCupsBetterCount.toDouble() / otherPlayers.size) * 1000) / 10.0
                } else 100.0

                val topCurrent = activePlayers.maxByOrNull { it.currentCups }
                val worstCurrent = activePlayers.minByOrNull { it.currentCups }
                val topMax = activePlayers.maxByOrNull { it.maxPossibleCups }
                val worstMax = activePlayers.minByOrNull { it.maxPossibleCups }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.75f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Tiêu đề & Nút Thu gọn / Mở rộng
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KỶ LỤC & THỐNG KÊ NỔI BẬT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.5.sp
                            )

                            val rotationAngle by animateFloatAsState(
                                targetValue = if (isHighlightsCollapsed) 180f else 0f,
                                animationSpec = tween(220),
                                label = "season_highlights_caret"
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { isHighlightsCollapsed = !isHighlightsCollapsed }
                                    .padding(horizontal = 9.dp, vertical = 4.5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (isHighlightsCollapsed) "Mở rộng" else "Thu gọn",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        painter = painterResource(id = R.drawable.ph_caret_up),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .graphicsLayer { rotationZ = rotationAngle }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = !isHighlightsCollapsed,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // 2 Thẻ Vị thế năng lực so với toàn bảng
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HighlightPositionCard(
                                        title = "VỊ THẾ CÚP HIỆN TẠI",
                                        percent = currentCupsPercentBetter,
                                        detail = if (myPlayer != null) "Vượt qua $currentCupsBetterCount/${otherPlayers.size} đối thủ" else "Chưa có dữ liệu",
                                        accentColor = Color(0xFF38BDF8),
                                        modifier = Modifier.weight(1f)
                                    )
                                    HighlightPositionCard(
                                        title = "VỊ THẾ TRẦN CÚP",
                                        percent = maxCupsPercentBetter,
                                        detail = if (myPlayer != null) "Vượt qua $maxCupsBetterCount/${otherPlayers.size} trần cúp" else "Chưa có dữ liệu",
                                        accentColor = GoldAccent,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // 4 Thẻ kỷ lục cúp bảng đấu (2x2)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        SeasonRecordCard(
                                            title = "CÚP HIỆN TẠI CAO NHẤT",
                                            value = if (topCurrent != null) "${NumberFormat.getNumberInstance(Locale.GERMANY).format(topCurrent.currentCups)} 🏆" else "--",
                                            holder = topCurrent?.name ?: "Chưa rõ",
                                            rank = if (topCurrent != null) "#${topCurrent.rank}" else "",
                                            color = GoldAccent,
                                            modifier = Modifier.weight(1f)
                                        )
                                        SeasonRecordCard(
                                            title = "CÚP HIỆN TẠI THẤP NHẤT",
                                            value = if (worstCurrent != null) "${NumberFormat.getNumberInstance(Locale.GERMANY).format(worstCurrent.currentCups)} 🏆" else "--",
                                            holder = worstCurrent?.name ?: "Chưa rõ",
                                            rank = if (worstCurrent != null) "#${worstCurrent.rank}" else "",
                                            color = RedAlarm,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        SeasonRecordCard(
                                            title = "TRẦN CÚP CAO NHẤT",
                                            value = if (topMax != null) "${NumberFormat.getNumberInstance(Locale.GERMANY).format(topMax.maxPossibleCups)} 🏆" else "--",
                                            holder = topMax?.name ?: "Chưa rõ",
                                            rank = if (topMax != null) "#${topMax.rank}" else "",
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.weight(1f)
                                        )
                                        SeasonRecordCard(
                                            title = "TRẦN CÚP THẤP NHẤT",
                                            value = if (worstMax != null) "${NumberFormat.getNumberInstance(Locale.GERMANY).format(worstMax.maxPossibleCups)} 🏆" else "--",
                                            holder = worstMax?.name ?: "Chưa rõ",
                                            rank = if (worstMax != null) "#${worstMax.rank}" else "",
                                            color = Color(0xFFF97316),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // MỤC 3: BẢNG XẾP HẠNG CHI TIẾT (PlayerTable)
            item {
                SearchAndFilterBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    selectedCategory = state.selectedCategory,
                    onCategorySelect = { viewModel.onCategorySelected(it) }
                )
            }

            // Tiêu đề danh sách người chơi
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BẢNG XẾP HẠNG CHI TIẾT",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${state.filteredPlayers.size} người chơi",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Danh sách người chơi
            items(state.filteredPlayers, key = { it.id }) { player ->
                PlayerItemCard(
                    player = player,
                    maxAttacks = season?.maxAttacks ?: 24,
                    maxDefenses = season?.maxDefenses ?: 24
                )
            }
        }
    }
}

@Composable
private fun RatingCountPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = color.copy(alpha = 0.9f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatusCountCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(
                text = "$count",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun HighlightPositionCard(
    title: String,
    percent: Double,
    detail: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.3.sp
            )
            Text(
                text = "Top ${"%.1f".format(percent)}%",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = detail,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SeasonRecordCard(
    title: String,
    value: String,
    holder: String,
    rank: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.2.sp
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (rank.isNotBlank()) {
                    Text(
                        text = rank,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = holder,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
