package com.cocrank.dashboard.ui.screens.personal

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cocrank.dashboard.R
import com.cocrank.dashboard.domain.model.RatingCategory
import com.cocrank.dashboard.ui.components.RatingBadge
import com.cocrank.dashboard.ui.components.StatCard
import com.cocrank.dashboard.ui.components.SyncPromptDialog
import com.cocrank.dashboard.ui.screens.dashboard.DashboardViewModel
import com.cocrank.dashboard.ui.theme.GoldAccent
import com.cocrank.dashboard.ui.theme.GreenSuperior
import com.cocrank.dashboard.ui.theme.RedAlarm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalScreen(
    viewModel: DashboardViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val my = state.stats?.myPlayer
    val stats = state.stats
    val season = state.season

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
                                text = "Thông Tin Cá Nhân",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (season != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GoldAccent.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = season.league,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (state.isSyncing) (state.syncMessage ?: "Đang đồng bộ...") else "Đồng bộ lúc: ${season?.lastSyncedAt ?: "Chưa rõ"}",
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
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painter = painterResource(id = R.drawable.ph_gear),
                            contentDescription = "Cài đặt",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            // Hero Player Profile Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent.copy(alpha = 0.15f))
                                        .border(1.dp, GoldAccent.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ph_user),
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = my?.name ?: "Tài khoản của bạn",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = my?.playerTag ?: state.savedPlayerTag.ifBlank { "#Chưa cài tag" },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (my != null) {
                                RatingBadge(category = my.rating)
                            }
                        }

                        if (my?.clanName != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ph_shield),
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Clan: ${my.clanName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Section 1: Thẻ chỉ số tổng quan
            item {
                Text(
                    text = "CHỈ SỐ THÀNH TÍCH",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Hạng & Cúp hiện tại
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Hạng hiện tại",
                            value = if (my != null) "#${my.rank}" else "--",
                            subtitle = if (my?.rankDiff != null && my.rankDiff != 0) {
                                if (my.rankDiff > 0) "Tăng ${my.rankDiff} bậc" else "Hạ ${-my.rankDiff} bậc"
                            } else "Chưa đổi hạng",
                            iconRes = R.drawable.ph_trophy,
                            accentColor = GoldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Cúp hiện tại",
                            value = if (my != null) "${my.currentCups} 🏆" else "--",
                            subtitle = my?.rating?.displayName ?: "Chưa xếp loại",
                            iconRes = R.drawable.ph_sparkle,
                            accentColor = GreenSuperior,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Trần cúp & Rank thấp nhất
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Trần cúp tối đa",
                            value = if (my != null) "${my.maxPossibleCups} 🏆" else "--",
                            subtitle = "Kịch bản thắng hoàn hảo",
                            iconRes = R.drawable.ph_shield,
                            accentColor = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Rank thấp nhất",
                            value = if (stats != null && stats.lowestPossibleRank > 0) "#${stats.lowestPossibleRank}" else "--",
                            subtitle = "Kịch bản xấu nhất",
                            iconRes = R.drawable.ph_trend_down,
                            accentColor = RedAlarm,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section 2: Hiệu suất & Đối thủ
            item {
                Text(
                    text = "HIỆU SUẤT & ĐỐI THỦ",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 2.dp, top = 6.dp, bottom = 2.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Đối thủ có thể vượt",
                        value = "${stats?.playersWhoCanPassMe ?: 0} người",
                        subtitle = "${stats?.playersDefinitelyBelowMe ?: 0} chắc chắn xếp dưới",
                        iconRes = R.drawable.ph_users,
                        accentColor = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Tiến độ lượt đấu",
                        value = if (my != null) "${my.attacks}/${season?.maxAttacks ?: 24} công" else "--",
                        subtitle = if (my != null) "Đã thủ: ${my.defenses}/${season?.maxDefenses ?: 24} trận" else "",
                        iconRes = R.drawable.ph_swords,
                        accentColor = Color(0xFFF97316),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 3: Thao tác & Cài đặt nhanh
            item {
                Text(
                    text = "TÙY CHỌN NHANH",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 2.dp, top = 6.dp, bottom = 2.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Đổi Player Tag
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openConfigDialog() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Đổi Player Tag",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = state.savedPlayerTag.ifBlank { "Chưa thiết lập" },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.ph_arrows_clockwise),
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        )

                        // Chế độ Giao diện Dark/Light
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Giao diện Tối (Dark Mode)",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (state.isDarkTheme) "Đang bật chế độ tối" else "Đang bật chế độ sáng",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.isDarkTheme,
                                onCheckedChange = { viewModel.setDarkTheme(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GoldAccent
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
