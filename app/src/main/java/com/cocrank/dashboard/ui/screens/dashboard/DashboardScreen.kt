package com.cocrank.dashboard.ui.screens.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cocrank.dashboard.ui.components.PlayerItemCard
import com.cocrank.dashboard.ui.components.SearchAndFilterBar
import com.cocrank.dashboard.ui.components.StatCard
import com.cocrank.dashboard.ui.components.SyncPromptDialog
import com.cocrank.dashboard.ui.theme.DarkBackground
import com.cocrank.dashboard.ui.theme.DarkSurface
import com.cocrank.dashboard.ui.theme.GoldAccent
import com.cocrank.dashboard.ui.theme.GreenSuperior
import com.cocrank.dashboard.ui.theme.PurpleDominant
import com.cocrank.dashboard.ui.theme.RedAlarm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrorMessage()
        }
    }

    if (state.showConfigDialog) {
        SyncPromptDialog(
            initialPlayerTag = state.savedPlayerTag,
            initialApiToken = state.savedApiToken,
            initialProxyUrl = state.savedProxyUrl,
            onDismiss = { viewModel.dismissConfigDialog() },
            onConfirm = { tag, token, proxy ->
                viewModel.saveConfig(tag, token, proxy)
            }
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
                                text = "CoC Rank",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                color = GoldAccent
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PurpleDominant.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = state.season?.league ?: "Legend",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleDominant
                                )
                            }
                        }
                        Text(
                            text = if (state.isSyncing) (state.syncMessage ?: "Đang đồng bộ...") else "Lần cuối: ${state.season?.lastSyncedAt ?: "Chưa rõ"}",
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
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Đồng bộ API",
                                tint = GoldAccent
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.openConfigDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Cấu hình",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stat Cards Grid
            item {
                val stats = state.stats
                val my = stats?.myPlayer
                val season = state.season

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Current Rank & Current Cups
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Hạng của bạn",
                            value = if (my != null) "#${my.rank}" else "--",
                            subtitle = if (my?.rankDiff != null && my.rankDiff != 0) {
                                if (my.rankDiff > 0) "Tăng ${my.rankDiff} bậc" else "Hạ ${-my.rankDiff} bậc"
                            } else "Chưa đổi",
                            icon = Icons.Default.EmojiEvents,
                            accentColor = GoldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Cúp hiện tại",
                            value = if (my != null) "${my.currentCups} 🏆" else "--",
                            subtitle = my?.rating?.displayName ?: "Chưa xếp loại",
                            icon = Icons.Default.SportsScore,
                            accentColor = GreenSuperior,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Max Cups & Lowest Possible Rank
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Cúp tối đa",
                            value = if (my != null) "${my.maxPossibleCups} 🏆" else "--",
                            subtitle = "Giả định toàn thắng & thủ 40c",
                            icon = Icons.Default.Security,
                            accentColor = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Rank thấp nhất",
                            value = if (stats != null && stats.lowestPossibleRank > 0) "#${stats.lowestPossibleRank}" else "--",
                            subtitle = "Dự đoán trường hợp xấu nhất",
                            icon = Icons.AutoMirrored.Filled.TrendingDown,
                            accentColor = RedAlarm,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 3: Pass me & Battles progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Có thể vượt bạn",
                            value = "${stats?.playersWhoCanPassMe ?: 0} người",
                            subtitle = "${stats?.playersDefinitelyBelowMe ?: 0} chắc chắn xếp dưới",
                            icon = Icons.Default.Group,
                            accentColor = Color(0xFFA855F7),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Lượt đánh / thủ",
                            value = if (my != null) "${my.attacks}/${season?.maxAttacks ?: 24}" else "--",
                            subtitle = if (my != null) "Đã thủ: ${my.defenses}/${season?.maxDefenses ?: 24}" else "",
                            accentColor = Color(0xFFF97316),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                SearchAndFilterBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    selectedCategory = state.selectedCategory,
                    onCategorySelect = { viewModel.onCategorySelected(it) }
                )
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BẢNG XẾP HẠNG MÙA GIẢI",
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

            // Players List
            items(state.filteredPlayers, key = { it.id }) { player ->
                PlayerItemCard(
                    player = player,
                    maxAttacks = state.season?.maxAttacks ?: 24,
                    maxDefenses = state.season?.maxDefenses ?: 24
                )
            }
        }
    }
}
