package com.cocrank.dashboard.ui.screens.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cocrank.dashboard.ui.screens.dashboard.DashboardViewModel
import com.cocrank.dashboard.ui.theme.DarkBackground
import com.cocrank.dashboard.ui.theme.DarkBorder
import com.cocrank.dashboard.ui.theme.DarkSurface
import com.cocrank.dashboard.ui.theme.GoldAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var playerTag by remember(state.savedPlayerTag) { mutableStateOf(state.savedPlayerTag) }
    var apiToken by remember(state.savedApiToken) { mutableStateOf(state.savedApiToken) }
    var proxyUrl by remember(state.savedProxyUrl) { mutableStateOf(state.savedProxyUrl) }
    var isDarkTheme by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cài Đặt & Cấu Hình",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSectionCard(title = "Tài Khoản Clash of Clans") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = playerTag,
                            onValueChange = { playerTag = it },
                            label = { Text("Mã Player Tag") },
                            placeholder = { Text("#90UQRP0") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = apiToken,
                            onValueChange = { apiToken = it },
                            label = { Text("Supercell API Token (Bearer)") },
                            placeholder = { Text("Nhập token nếu gọi trực tiếp") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = proxyUrl,
                            onValueChange = { proxyUrl = it },
                            label = { Text("Proxy URL Backend (Tùy chọn)") },
                            placeholder = { Text("https://cocproxy.royaleapi.dev/v1") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                viewModel.saveConfig(playerTag, apiToken, proxyUrl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Lưu Thay Đổi", color = DarkSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Giao Diện & Trải Nghiệm") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Chế độ Gaming Dark Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Giao diện tối màu tương phản cao",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { isDarkTheme = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent)
                        )
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Thông Tin Ứng Dụng") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CoC Rank Dashboard Mobile v1.0.0",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hệ thống theo dõi thứ hạng và phân tích phong độ giải đấu Clash of Clans xây dựng bằng Kotlin & Jetpack Compose.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = GoldAccent,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
