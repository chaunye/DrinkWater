package com.drinkwater.ui.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drinkwater.DrinkWaterApp
import com.drinkwater.data.db.DailyActionCount
import com.drinkwater.data.model.ReminderAction
import com.drinkwater.util.TimeUtil
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as DrinkWaterApp).database
    val logDao = db.reminderLogDao()

    val startOfDay = TimeUtil.getStartOfDay()
    val endOfDay = TimeUtil.getEndOfDay()
    val weekAgo = TimeUtil.getDaysAgo(7)

    val todayTotal by logDao.getTodayCount(startOfDay, endOfDay).collectAsState(initial = 0)
    val todayConfirmed by logDao.getTodayCountByAction(startOfDay, endOfDay, ReminderAction.CONFIRMED).collectAsState(initial = 0)
    val todayCancelled by logDao.getTodayCountByAction(startOfDay, endOfDay, ReminderAction.CANCELLED).collectAsState(initial = 0)
    val dailyData by logDao.getDailyCountsByAction(weekAgo).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "DrinkWater",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "你的私人自律管家",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Must Read section
        MustReadCard()

        Spacer(modifier = Modifier.height(24.dp))

        // Today's stats
        Text("今日统计", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "被拦截",
                value = todayTotal.toString(),
                color = Color(0xFF2196F3),
                icon = Icons.Default.Shield
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "确认打开",
                value = todayConfirmed.toString(),
                color = Color(0xFFFF9800),
                icon = Icons.Default.CheckCircle
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "已取消",
                value = todayCancelled.toString(),
                color = Color(0xFF4CAF50),
                icon = Icons.Default.Cancel
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 7-day trend
        Text("7天趋势", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (dailyData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("暂无数据", color = Color.Gray)
                    }
                }
            } else {
                TrendChart(
                    data = dailyData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MustReadCard() {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("用前必看", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFFE65100)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                MustReadItem(Icons.Default.Visibility, "无障碍权限", "首次使用请在设置中开启无障碍服务，否则无法检测应用启动。")
                MustReadItem(Icons.Default.Notifications, "通知权限", "开启通知以接收定时提醒和背词提醒。")
                MustReadItem(Icons.Default.Add, "添加监控", "在「监控」页面点击 + 添加你想拦截的应用，每条默认提醒是「你今天喝水了吗？」。")
                MustReadItem(Icons.Default.MenuBook, "生词本", "在「生词本」页面导入 Excel/CSV/TXT 词表，背词提醒会和应用监控联动。")
                MustReadItem(Icons.Default.Schedule, "定时提醒", "在「监控」页面配置应用时可设置生效时段，支持全天候或自定义时间段。")
                MustReadItem(Icons.Default.Fullscreen, "弹窗模式", "在「设置」中可切换全屏弹窗或悬浮窗模式。")

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "隐私声明：本应用完全离线运行，不联网，不收集任何数据。你的数据只有你自己和老天知道，绝对不会泄露。",
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MustReadItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE65100))
            Text(desc, fontSize = 12.sp, color = Color(0xFF795548))
        }
    }
}

@Composable
fun TrendChart(data: List<DailyActionCount>, modifier: Modifier = Modifier) {
    val grouped = data.groupBy { it.day }
    val days = grouped.keys.sorted()
    val totals = days.map { day -> grouped[day]?.sumOf { it.count } ?: 0 }
    val maxVal = (totals.maxOrNull() ?: 1).coerceAtLeast(1)

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        if (days.size < 2) return@Canvas
        val stepX = size.width / (days.size - 1).coerceAtLeast(1)
        val points = totals.mapIndexed { index, value ->
            Offset(
                x = index * stepX,
                y = size.height * (1 - value.toFloat() / maxVal)
            )
        }

        // Draw line
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw dots
        points.forEach { point ->
            drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = point)
        }
    }
}
