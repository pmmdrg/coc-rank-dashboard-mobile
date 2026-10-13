package com.cocrank.dashboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cocrank.dashboard.ui.theme.GoldAccent

@Composable
fun SyncPromptDialog(
    initialPlayerTag: String,
    onDismiss: () -> Unit,
    onConfirm: (playerTag: String) -> Unit
) {
    var playerTag by remember { mutableStateOf(initialPlayerTag) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = GoldAccent,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Đồng Bộ Thứ Hạng CoC",
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
                    text = "Nhập mã Player Tag (#TAG) của bạn trong Clash of Clans để tự động cập nhật bảng xếp hạng và điểm cúp mùa giải thực tế.",
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
                    shape = RoundedCornerShape(8.dp),
                    colors = textFieldColors
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(playerTag) },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
            ) {
                Text("Lưu & Đồng bộ", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
