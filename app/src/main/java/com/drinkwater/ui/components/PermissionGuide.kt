package com.drinkwater.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drinkwater.data.db.SettingsDataStore
import com.drinkwater.service.AppMonitorService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PermissionGuideScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })

    val serviceRunning = AppMonitorService.isRunning()
    val canOverlay = remember { Settings.canDrawOverlays(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "DrinkWater",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "你的私人自律管家",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Privacy notice
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(12.dp))
                Text("本应用不联网，所有数据仅存本地，请放心使用", fontSize = 14.sp, color = Color(0xFF2E7D32))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> GuidePage(
                    icon = Icons.Default.Visibility,
                    title = "无障碍权限",
                    description = "DrinkWater 需要无障碍权限来检测您打开的应用，以便在合适时机提醒您。",
                    actionText = if (serviceRunning) "已开启" else "前往开启",
                    isDone = serviceRunning,
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
                1 -> GuidePage(
                    icon = Icons.Default.Layers,
                    title = "悬浮窗权限",
                    description = "开启悬浮窗权限后，提醒弹窗可以显示在其他应用上方，不会被遮挡。",
                    actionText = if (canOverlay) "已开启" else "前往开启",
                    isDone = canOverlay,
                    onAction = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )
                2 -> GuidePage(
                    icon = Icons.Default.Notifications,
                    title = "通知权限",
                    description = "开启通知权限以接收定时提醒和背词提醒。",
                    actionText = "前往开启",
                    isDone = false,
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        })
                    }
                )
                3 -> GuidePage(
                    icon = Icons.Default.Celebration,
                    title = "设置完成！",
                    description = "现在您可以开始添加要监控的应用了。\n\n点击首页右上角 + 添加应用，设置您的第一条提醒吧！",
                    actionText = "开始使用",
                    isDone = false,
                    onAction = {
                        scope.launch {
                            settings.setFirstLaunchDone()
                        }
                        onComplete()
                    }
                )
            }
        }

        // Page indicator
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { index ->
                val isSelected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 8.dp,
                    animationSpec = tween(durationMillis = 300),
                    label = "indicator_width"
                )
                val color by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                    animationSpec = tween(durationMillis = 300),
                    label = "indicator_color"
                )
                Box(
                    modifier = Modifier
                        .size(width, 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pagerState.currentPage < 3) {
            TextButton(onClick = {
                scope.launch { settings.setFirstLaunchDone() }
                onComplete()
            }) {
                Text("跳过", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GuidePage(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String,
    isDone: Boolean,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = if (isDone) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            description,
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (isDone) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(8.dp))
                Text("已开启", color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
            }
        } else {
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(actionText, fontSize = 16.sp)
            }
        }
    }
}
