package com.cocrank.dashboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cocrank.dashboard.ui.theme.DarkSurface
import com.cocrank.dashboard.ui.theme.GoldAccent

@Composable
fun SyncPromptDialog(
    initialPlayerTag: String,
    initialApiToken: String,
    initialProxyUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (playerTag: String, apiToken: String, proxyUrl: String) -> Unit
) {
    var playerTag by remember { mutableStateOf(initialPlayerTag) }
    var apiToken by remember { mutableStateOf(initialApiToken) }
    var proxyUrl by remember { mutableStateOf(initialProxyUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Cấu hình Đồng bộ CoC",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Nhập mã Player Tag của bạn trong Clash of Clans để đồng bộ bảng đấu và điểm số cúp thực tế.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = playerTag,
                    onValueChange = { playerTag = it },
                    label = { Text("Mã Player Tag (#TAG)") },
                    placeholder = { Text("#90UQRP0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = apiToken,
                    onValueChange = { apiToken = it },
                    label = { Text("CoC API Token (Tùy chọn)") },
                    placeholder = { Text("Bearer Token từ developer.clashofclans.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = proxyUrl,
                    onValueChange = { proxyUrl = it },
                    label = { Text("Proxy URL (Tùy chọn)") },
                    placeholder = { Text("https://cocproxy.royaleapi.dev/v1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(playerTag, apiToken, proxyUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
            ) {
                Text("Lưu & Đồng bộ", color = DarkSurface, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
