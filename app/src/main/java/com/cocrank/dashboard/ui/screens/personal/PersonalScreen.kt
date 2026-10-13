package com.cocrank.dashboard.ui.screens.personal

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cocrank.dashboard.R
import com.cocrank.dashboard.data.LeagueIcons
import com.cocrank.dashboard.domain.usecase.RankingCalculator
import com.cocrank.dashboard.ui.components.RatingBadge
import com.cocrank.dashboard.ui.screens.dashboard.DashboardViewModel
import com.cocrank.dashboard.ui.theme.GoldAccent
import com.cocrank.dashboard.ui.theme.GreenSuperior
import com.cocrank.dashboard.ui.theme.RedAlarm
import java.text.NumberFormat
import java.util.Locale

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
    val context = LocalContext.current

    var isOverviewCollapsed by remember { mutableStateOf(false) }
    var isTrendCollapsed by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Thông Tin Cá Nhân",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
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
            // Hero Player Profile Banner (Chuyển thông tin League xuống thẻ thông tin người dùng)
            item {
                val leagueIconUrl = LeagueIcons.getLeagueIconUrl(season?.league, season?.leagueIconUrl)

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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                // League Badge Icon (Huy hiệu giải đấu)
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(GoldAccent.copy(alpha = 0.12f))
                                        .border(1.dp, GoldAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(leagueIconUrl)
                                            .crossfade(true)
                                            .error(R.drawable.ph_trophy)
                                            .placeholder(R.drawable.ph_trophy)
                                            .build(),
                                        contentDescription = season?.league ?: "Huy hiệu giải đấu",
                                        modifier = Modifier.size(40.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = my?.name ?: "Tài khoản của bạn",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        // Huy hiệu League Tier chuyển xuống đây
                                        if (season != null && season.league.isNotBlank() && season.league != "--") {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(GoldAccent.copy(alpha = 0.15f))
                                                    .border(1.dp, GoldAccent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
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
                                    Spacer(modifier = Modifier.height(2.dp))
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

            // Section 1: CHỈ SỐ TỔNG QUAN CÁ NHÂN (Thiết kế chuẩn hóa theo dashboard web)
            item {
                val hasTournamentGroup = (season?.players?.size ?: 0) > 1
                val totalPlayers = season?.players?.size ?: 0
                val myRankPercentage = if (my != null && my.rank > 0 && hasTournamentGroup && totalPlayers > 0) {
                    Math.round((my.rank.toDouble() / totalPlayers) * 100)
                } else null

                val myName = my?.name ?: "Bạn"
                val myCupsFormatted = NumberFormat.getNumberInstance(Locale.GERMANY).format(my?.currentCups ?: 0)
                val maxCupsFormatted = NumberFormat.getNumberInstance(Locale.GERMANY).format(my?.maxPossibleCups ?: 0)

                // Dữ liệu Card 1: Thứ hạng hiện tại
                val isRankValid = my != null && my.rank > 0 && hasTournamentGroup
                val rankValue = if (isRankValid) "#${my!!.rank}" else "--"
                val rankBadge = if (myRankPercentage != null) "Top $myRankPercentage%" else null
                val rankDetail = if (my != null) "$myName • $myCupsFormatted cúp" else "Chưa có dữ liệu"

                // Dữ liệu Card 2: Đối thủ có thể vượt
                val canPassCount = stats?.playersWhoCanPassMe ?: 0
                val threatBadge = if (hasTournamentGroup) {
                    if (canPassCount == 0) "An toàn" else "$canPassCount nguy cơ"
                } else null
                val threatColor = if (canPassCount == 0) Color(0xFF10B981) else Color(0xFFF97316)
                val threatValue = if (hasTournamentGroup) "$canPassCount" else "--"
                val threatDetail = if (my != null) "Trần cup của $myName: $maxCupsFormatted" else "Chưa có dữ liệu"

                // Dữ liệu Card 3: Tấn công
                val myAtkCount = my?.attacks ?: 0
                val myAtkDest = my?.attackDestruction ?: 0.0
                val atkStars = my?.attackAvgStars ?: if (myAtkCount > 0 && myAtkDest > 0.0) {
                    if (myAtkDest >= 100) 3.0 else if (myAtkDest >= 70) 2.5 else if (myAtkDest >= 50) 2.0 else 1.0
                } else null
                val atkBadge = if (atkStars != null && atkStars > 0) "★ ${"%.1f".format(atkStars)}/trận" else null
                val atkValue = if (myAtkCount > 0 || myAtkDest > 0.0) "${"%.1f".format(myAtkDest)}%" else "--"
                val atkDetail = if (my != null) "$myAtkCount trận công" else "Chưa có dữ liệu"

                // Dữ liệu Card 4: Phòng thủ
                val myDefCount = my?.defenses ?: 0
                val myDefDest = my?.defenseDestruction ?: 0.0
                val defStars = my?.defenseAvgStars ?: if (myDefCount > 0 && myDefDest > 0.0) {
                    if (myDefDest >= 100) 3.0 else if (myDefDest >= 70) 2.5 else if (myDefDest >= 50) 2.0 else 1.0
                } else null
                val defBadge = if (defStars != null && defStars > 0) "★ ${"%.1f".format(defStars)}/trận" else null
                val defValue = if (myDefCount > 0 || myDefDest > 0.0) "${"%.1f".format(myDefDest)}%" else "--"
                val defDetail = if (my != null) "$myDefCount trận thủ" else "Chưa có dữ liệu"

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
                                text = "CHỈ SỐ TỔNG QUAN CÁ NHÂN",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.5.sp
                            )

                            val rotationAngle by animateFloatAsState(
                                targetValue = if (isOverviewCollapsed) 180f else 0f,
                                animationSpec = tween(220),
                                label = "caret_rotation"
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

                        // Lưới 4 thẻ con (2 hàng x 2 cột)
                        AnimatedVisibility(
                            visible = !isOverviewCollapsed,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Hàng 1: Thứ hạng hiện tại & Đối thủ có thể vượt
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PersonalOverviewCard(
                                        label = "THỨ HẠNG HIỆN TẠI",
                                        badgeText = rankBadge,
                                        badgeColor = Color(0xFF38BDF8),
                                        badgeBgColor = Color(0xFF38BDF8).copy(alpha = 0.14f),
                                        value = rankValue,
                                        valueColor = Color(0xFF38BDF8),
                                        detailText = rankDetail,
                                        accentGradient = Brush.horizontalGradient(
                                            listOf(Color(0xFF38BDF8), Color(0xFF6366F1))
                                        ),
                                        watermarkIconRes = R.drawable.ph_trophy,
                                        watermarkColor = Color(0xFF38BDF8),
                                        modifier = Modifier.weight(1f)
                                    )

                                    PersonalOverviewCard(
                                        label = "ĐỐI THỦ CÓ THỂ VƯỢT",
                                        badgeText = threatBadge,
                                        badgeColor = threatColor,
                                        badgeBgColor = threatColor.copy(alpha = 0.14f),
                                        value = threatValue,
                                        valueColor = threatColor,
                                        detailText = threatDetail,
                                        accentGradient = Brush.horizontalGradient(
                                            if (canPassCount == 0) listOf(Color(0xFF10B981), Color(0xFF06B6D4))
                                            else listOf(Color(0xFFF97316), Color(0xFFEF4444))
                                        ),
                                        watermarkIconRes = R.drawable.ph_shield,
                                        watermarkColor = threatColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Hàng 2: Tấn công & Phòng thủ
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PersonalOverviewCard(
                                        label = "TẤN CÔNG",
                                        badgeText = atkBadge,
                                        badgeColor = Color(0xFFFBBF24),
                                        badgeBgColor = Color(0xFFFBBF24).copy(alpha = 0.14f),
                                        value = atkValue,
                                        valueColor = Color(0xFFFBBF24),
                                        detailText = atkDetail,
                                        accentGradient = Brush.horizontalGradient(
                                            listOf(Color(0xFFFBBF24), Color(0xFFF97316))
                                        ),
                                        watermarkIconRes = R.drawable.ph_swords,
                                        watermarkColor = Color(0xFFFBBF24),
                                        modifier = Modifier.weight(1f)
                                    )

                                    PersonalOverviewCard(
                                        label = "PHÒNG THỦ",
                                        badgeText = defBadge,
                                        badgeColor = Color(0xFF10B981),
                                        badgeBgColor = Color(0xFF10B981).copy(alpha = 0.14f),
                                        value = defValue,
                                        valueColor = Color(0xFF10B981),
                                        detailText = defDetail,
                                        accentGradient = Brush.horizontalGradient(
                                            listOf(Color(0xFF14B8A6), Color(0xFF10B981))
                                        ),
                                        watermarkIconRes = R.drawable.ph_shield,
                                        watermarkColor = Color(0xFF10B981),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: THEO DÕI PHONG ĐỘ & KỶ LỤC (Performance Trend & Records)
            item {
                val leagueHistory = state.leagueHistory
                val bestRankItem = RankingCalculator.findBestRankSeason(leagueHistory)
                val bestCupsItem = RankingCalculator.findBestCupsSeason(leagueHistory)
                val latestSeasonItem = leagueHistory.maxByOrNull { it.leagueSeasonId }

                val totalWins = leagueHistory.sumOf { it.attackWins }
                val totalAttacks = leagueHistory.sumOf { it.attackWins + it.attackLosses }
                val overallWinRate = if (totalAttacks > 0) Math.round((totalWins.toDouble() / totalAttacks) * 1000) / 10.0 else 0.0

                val bestRankVal = if (bestRankItem != null) "#${bestRankItem.placement}" else "--"
                val bestRankDetail = if (bestRankItem != null) "${bestRankItem.tierName} • ${bestRankItem.displayPeriod}" else "Chưa có dữ liệu"

                val bestCupsVal = if (bestCupsItem != null) NumberFormat.getNumberInstance(Locale.GERMANY).format(bestCupsItem.leagueTrophies) else "--"
                val bestCupsDetail = if (bestCupsItem != null) "${bestCupsItem.tierName} • ${bestCupsItem.displayPeriod}" else "Chưa có dữ liệu"

                val latestVal = if (latestSeasonItem != null) NumberFormat.getNumberInstance(Locale.GERMANY).format(latestSeasonItem.leagueTrophies) else "--"
                val latestDetail = if (latestSeasonItem != null) "Hạng #${latestSeasonItem.placement} • ${latestSeasonItem.displayPeriod}" else "Chưa có dữ liệu"

                val winRateVal = if (totalAttacks > 0) "${overallWinRate}%" else "--"
                val winRateDetail = if (totalAttacks > 0) "$totalWins thắng / $totalAttacks trận" else "Chưa có dữ liệu"

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
                                text = "THEO DÕI PHONG ĐỘ & KỶ LỤC",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.5.sp
                            )

                            val rotationAngle by animateFloatAsState(
                                targetValue = if (isTrendCollapsed) 180f else 0f,
                                animationSpec = tween(220),
                                label = "trend_caret_rotation"
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
                                    .clickable { isTrendCollapsed = !isTrendCollapsed }
                                    .padding(horizontal = 9.dp, vertical = 4.5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (isTrendCollapsed) "Mở rộng" else "Thu gọn",
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

                        // Lưới 4 thẻ kỷ lục & Danh sách các mùa giải gần đây
                        AnimatedVisibility(
                            visible = !isTrendCollapsed,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // 4 Thẻ kỷ lục con
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        PersonalOverviewCard(
                                            label = "KỶ LỤC THỨ HẠNG",
                                            badgeText = if (bestRankItem != null) "Tốt nhất" else null,
                                            badgeColor = Color(0xFFA855F7),
                                            badgeBgColor = Color(0xFFA855F7).copy(alpha = 0.14f),
                                            value = bestRankVal,
                                            valueColor = Color(0xFFA855F7),
                                            detailText = bestRankDetail,
                                            accentGradient = Brush.horizontalGradient(
                                                listOf(Color(0xFFA855F7), Color(0xFF6366F1))
                                            ),
                                            watermarkIconRes = R.drawable.ph_trophy,
                                            watermarkColor = Color(0xFFA855F7),
                                            modifier = Modifier.weight(1f)
                                        )

                                        PersonalOverviewCard(
                                            label = "KỶ LỤC CÚP",
                                            badgeText = if (bestCupsItem != null) "Đỉnh cao" else null,
                                            badgeColor = GoldAccent,
                                            badgeBgColor = GoldAccent.copy(alpha = 0.14f),
                                            value = bestCupsVal,
                                            valueColor = GoldAccent,
                                            detailText = bestCupsDetail,
                                            accentGradient = Brush.horizontalGradient(
                                                listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
                                            ),
                                            watermarkIconRes = R.drawable.ph_sparkle,
                                            watermarkColor = GoldAccent,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        PersonalOverviewCard(
                                            label = "MÙA GẦN NHẤT",
                                            badgeText = if (latestSeasonItem != null) "Gần đây" else null,
                                            badgeColor = Color(0xFF38BDF8),
                                            badgeBgColor = Color(0xFF38BDF8).copy(alpha = 0.14f),
                                            value = latestVal,
                                            valueColor = Color(0xFF38BDF8),
                                            detailText = latestDetail,
                                            accentGradient = Brush.horizontalGradient(
                                                listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                            ),
                                            watermarkIconRes = R.drawable.ph_chart_line_up,
                                            watermarkColor = Color(0xFF38BDF8),
                                            modifier = Modifier.weight(1f)
                                        )

                                        PersonalOverviewCard(
                                            label = "TỶ LỆ THẮNG TỔNG",
                                            badgeText = if (totalAttacks > 0) "Thực chiến" else null,
                                            badgeColor = GreenSuperior,
                                            badgeBgColor = GreenSuperior.copy(alpha = 0.14f),
                                            value = winRateVal,
                                            valueColor = GreenSuperior,
                                            detailText = winRateDetail,
                                            accentGradient = Brush.horizontalGradient(
                                                listOf(Color(0xFF10B981), Color(0xFF059669))
                                            ),
                                            watermarkIconRes = R.drawable.ph_swords,
                                            watermarkColor = GreenSuperior,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Danh sách lịch sử các mùa giải
                                if (leagueHistory.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "LỊCH SỬ CÁC MÙA GIẢI GẦN ĐÂY",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = 0.3.sp
                                        )

                                        leagueHistory.take(4).forEach { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.displayPeriod,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = item.tierName,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = "${NumberFormat.getNumberInstance(Locale.GERMANY).format(item.leagueTrophies)} 🏆",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GoldAccent
                                                    )

                                                    if (item.placement > 0) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                                                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "#${item.placement}",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF38BDF8)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thẻ chỉ số tổng quan con mang thiết kế Apple Specular Bar, số lớn font đậm và biểu tượng chìm
 */
@Composable
private fun PersonalOverviewCard(
    label: String,
    badgeText: String?,
    badgeColor: Color,
    badgeBgColor: Color,
    value: String,
    valueColor: Color,
    detailText: String,
    accentGradient: Brush,
    watermarkIconRes: Int,
    watermarkColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // Specular Top Accent Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.5.dp)
                .background(accentGradient)
        )

        // Watermark Icon ở góc dưới bên phải
        Icon(
            painter = painterResource(id = watermarkIconRes),
            contentDescription = null,
            tint = watermarkColor.copy(alpha = 0.08f),
            modifier = Modifier
                .size(68.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 10.dp)
        )

        // Nội dung thẻ
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            // Hàng nhãn tiêu đề + Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!badgeText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeBgColor)
                            .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Giá trị chính
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Chi tiết phụ bên dưới
            Text(
                text = detailText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
