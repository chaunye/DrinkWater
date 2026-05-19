package com.drinkwater.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drinkwater.DrinkWaterApp
import com.drinkwater.data.db.SettingsDataStore
import com.drinkwater.service.AppMonitorService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()

    val popupMode by settings.popupMode.collectAsState(initial = "fullscreen")
    val delayMinutes by settings.delayMinutes.collectAsState(initial = 5)
    val notificationsEnabled by settings.notificationsEnabled.collectAsState(initial = true)
    val serviceRunning = AppMonitorService.isRunning()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(title = { Text("设置") })

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Accessibility service status
            SectionTitle("无障碍服务")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (serviceRunning) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFFF9800).copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (serviceRunning) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (serviceRunning) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (serviceRunning) "无障碍服务已开启" else "无障碍服务未开启",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (serviceRunning) "应用监控正常运行中" else "点击前往设置开启",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Popup mode
            SectionTitle("弹窗模式")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fullscreen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("全屏弹窗", modifier = Modifier.weight(1f))
                        RadioButton(
                            selected = popupMode == "fullscreen",
                            onClick = { scope.launch { settings.setPopupMode("fullscreen") } }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureInPicture, contentDescription = null, tint = Color(0xFFFF9800))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("悬浮窗", modifier = Modifier.weight(1f))
                        RadioButton(
                            selected = popupMode == "floating",
                            onClick = { scope.launch { settings.setPopupMode("floating") } }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Delay time
            SectionTitle("延迟提醒时长")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFFF9800))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("延迟时间", modifier = Modifier.weight(1f))
                    listOf(3, 5, 10, 15, 30).forEach { minutes ->
                        FilterChip(
                            selected = delayMinutes == minutes,
                            onClick = { scope.launch { settings.setDelayMinutes(minutes) } },
                            label = { Text("${minutes}分") },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notifications
            SectionTitle("通知")
            SettingsToggle(
                icon = Icons.Default.Notifications,
                title = "定时提醒通知",
                subtitle = "接收定时提醒和背词提醒",
                checked = notificationsEnabled,
                onCheckedChange = { scope.launch { settings.setNotificationsEnabled(it) } }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // About
            SectionTitle("关于")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DrinkWater v1.0.0", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("开源离线自律提醒工具", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("本应用不联网，所有数据仅存本地", fontSize = 12.sp, color = Color(0xFF4CAF50))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
