package com.drinkwater.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drinkwater.DrinkWaterApp
import com.drinkwater.data.db.SettingsDataStore
import com.drinkwater.data.model.TodoItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as DrinkWaterApp).database
    val scope = rememberCoroutineScope()
    val todoDao = db.todoItemDao()
    val settings = remember { SettingsDataStore(context) }

    val todoItems by todoDao.getAll().collectAsState(initial = emptyList())
    val globalTimeEnabled by settings.globalTimeEnabled.collectAsState(initial = false)
    val globalStartTime by settings.globalStartTime.collectAsState(initial = "08:00")
    val globalEndTime by settings.globalEndTime.collectAsState(initial = "22:00")

    var showAddDialog by remember { mutableStateOf(false) }
    var showTimeSettings by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<TodoItem?>(null) }

    val uncompletedCount = todoItems.count { !it.isCompleted }
    val completedCount = todoItems.count { it.isCompleted }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("DrinkWater", fontWeight = FontWeight.Bold)
                    Text("待办提醒", fontSize = 12.sp, color = Color.Gray)
                }
            },
            actions = {
                IconButton(onClick = { showTimeSettings = true }) {
                    Icon(Icons.Default.Schedule, contentDescription = "时段设置")
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }
        )

        if (todoItems.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("还没有待办提醒", color = Color.Gray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("点击右上角 + 添加提醒事项", color = Color.LightGray, fontSize = 14.sp)
                }
            }
        } else {
            // Stats bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip(
                    text = "待完成 $uncompletedCount",
                    color = Color(0xFF2196F3),
                    icon = Icons.Default.RadioButtonUnchecked
                )
                StatChip(
                    text = "已完成 $completedCount",
                    color = Color(0xFF4CAF50),
                    icon = Icons.Default.CheckCircle
                )
                Spacer(modifier = Modifier.weight(1f))
                if (completedCount > 0) {
                    TextButton(onClick = {
                        scope.launch { todoDao.deleteCompleted() }
                    }) {
                        Text("清除已完成", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            // Global apply button
            if (uncompletedCount > 0) {
                Button(
                    onClick = {
                        scope.launch {
                            val uncompleted = todoDao.getAllUncompleted()
                            val monitoredApps = db.monitoredAppDao().getAllOnce()
                            uncompleted.forEach { todo ->
                                if (todo.applyToAll) {
                                    monitoredApps.forEach { app ->
                                        val existing = db.reminderDao().getByAppOnce(app.packageName)
                                        if (existing.none { it.content == todo.content }) {
                                            db.reminderDao().insert(
                                                com.drinkwater.data.model.Reminder(
                                                    appPackageName = app.packageName,
                                                    content = todo.content
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("同步到所有监控应用")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Todo list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todoItems, key = { it.id }) { item ->
                    TodoItemCard(
                        item = item,
                        onToggle = {
                            scope.launch {
                                todoDao.update(item.copy(isCompleted = !item.isCompleted))
                            }
                        },
                        onClick = { editingItem = item },
                        onDelete = {
                            scope.launch { todoDao.delete(item) }
                        }
                    )
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        AddTodoDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { content, applyToAll, isAllDay, startTime, endTime ->
                scope.launch {
                    todoDao.insert(
                        TodoItem(
                            content = content,
                            applyToAll = applyToAll,
                            isAllDay = isAllDay,
                            startTime = startTime,
                            endTime = endTime
                        )
                    )
                }
                showAddDialog = false
            }
        )
    }

    // Edit dialog
    editingItem?.let { item ->
        EditTodoDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                scope.launch { todoDao.update(updated) }
                editingItem = null
            }
        )
    }

    // Time settings dialog
    if (showTimeSettings) {
        GlobalTimeSettingsDialog(
            enabled = globalTimeEnabled,
            startTime = globalStartTime,
            endTime = globalEndTime,
            onDismiss = { showTimeSettings = false },
            onSave = { enabled, start, end ->
                scope.launch {
                    settings.setGlobalTimeEnabled(enabled)
                    settings.setGlobalStartTime(start)
                    settings.setGlobalEndTime(end)
                }
                showTimeSettings = false
            }
        )
    }
}

@Composable
fun StatChip(text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun TodoItemCard(
    item: TodoItem,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val alpha = if (item.isCompleted) 0.5f else 1f
    val textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) Color(0xFFF5F5F5) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF4CAF50)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.content,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = textDecoration,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.applyToAll) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2196F3).copy(alpha = 0.1f)
                        ) {
                            Text(
                                "全局",
                                fontSize = 11.sp,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!item.isAllDay) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFF9800).copy(alpha = 0.1f)
                        ) {
                            Text(
                                "${item.startTime}-${item.endTime}",
                                fontSize = 11.sp,
                                color = Color(0xFFFF9800),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "删除", modifier = Modifier.size(16.dp), tint = Color.Gray)
            }
        }
    }
}

@Composable
fun AddTodoDialog(
    onDismiss: () -> Unit,
    onAdd: (content: String, applyToAll: Boolean, isAllDay: Boolean, startTime: String, endTime: String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var applyToAll by remember { mutableStateOf(false) }
    var isAllDay by remember { mutableStateOf(true) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("22:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加提醒") },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("输入提醒内容...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SyncAlt, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("应用到所有监控应用", modifier = Modifier.weight(1f))
                    Switch(
                        checked = applyToAll,
                        onCheckedChange = { applyToAll = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("全天有效", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it }
                    )
                }

                if (!isAllDay) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("开始") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("结束") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (content.isNotBlank()) {
                        onAdd(content.trim(), applyToAll, isAllDay, startTime, endTime)
                    }
                },
                enabled = content.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun EditTodoDialog(
    item: TodoItem,
    onDismiss: () -> Unit,
    onSave: (TodoItem) -> Unit
) {
    var content by remember { mutableStateOf(item.content) }
    var applyToAll by remember { mutableStateOf(item.applyToAll) }
    var isAllDay by remember { mutableStateOf(item.isAllDay) }
    var startTime by remember { mutableStateOf(item.startTime) }
    var endTime by remember { mutableStateOf(item.endTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑提醒") },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SyncAlt, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("应用到所有监控应用", modifier = Modifier.weight(1f))
                    Switch(
                        checked = applyToAll,
                        onCheckedChange = { applyToAll = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("全天有效", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it }
                    )
                }

                if (!isAllDay) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("开始") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("结束") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (content.isNotBlank()) {
                        onSave(
                            item.copy(
                                content = content.trim(),
                                applyToAll = applyToAll,
                                isAllDay = isAllDay,
                                startTime = startTime,
                                endTime = endTime
                            )
                        )
                    }
                },
                enabled = content.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun GlobalTimeSettingsDialog(
    enabled: Boolean,
    startTime: String,
    endTime: String,
    onDismiss: () -> Unit,
    onSave: (Boolean, String, String) -> Unit
) {
    var isEnabled by remember { mutableStateOf(enabled) }
    var start by remember { mutableStateOf(startTime) }
    var end by remember { mutableStateOf(endTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("全局时段设置") },
        text = {
            Column {
                Text(
                    "设置所有待办提醒的默认生效时段。单独设置了时段的待办会覆盖此设置。",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("启用全局时段", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }

                if (isEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = start,
                            onValueChange = { start = it },
                            label = { Text("开始时间") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = end,
                            onValueChange = { end = it },
                            label = { Text("结束时间") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(isEnabled, start, end) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun MustReadCard() {
    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context) }
    val mustReadDismissed by settings.mustReadDismissed.collectAsState(initial = false)
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val cardColor = if (mustReadDismissed) Color(0xFFF5F5F5) else Color(0xFFFFF3E0)
    val titleColor = if (mustReadDismissed) Color.Gray else Color(0xFFE65100)
    val iconColor = if (mustReadDismissed) Color.Gray else Color(0xFFFF9800)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("用前必看", fontSize = if (mustReadDismissed) 16.sp else 18.sp, fontWeight = if (mustReadDismissed) FontWeight.Medium else FontWeight.Bold, color = titleColor)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = titleColor
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                MustReadItem(Icons.Default.Visibility, "无障碍权限", "首次使用请在设置中开启无障碍服务，否则无法检测应用启动。")
                MustReadItem(Icons.Default.Layers, "悬浮窗权限", "开启悬浮窗权限后，提醒弹窗可以显示在其他应用上方，不会被遮挡。")
                MustReadItem(Icons.Default.Notifications, "通知权限", "开启通知以接收定时提醒和背词提醒。")
                MustReadItem(Icons.Default.Add, "添加监控", "在「监控」页面点击 + 添加你想拦截的应用，每条默认提醒是「你今天喝水了吗？」。")
                MustReadItem(Icons.Default.MenuBook, "生词本", "在「生词本」页面导入 Excel/CSV/TXT 词表，背词提醒会和应用监控联动。")
                MustReadItem(Icons.Default.Schedule, "定时提醒", "在「监控」页面配置应用时可设置生效时段，支持全天候或自定义时间段。")
                MustReadItem(Icons.Default.PictureInPicture, "弹窗模式", "在「设置」中可切换弹窗模式或全屏模式，推荐使用弹窗模式。")

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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            settings.setMustReadDismissed()
                        }
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mustReadDismissed) Color.Gray else Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (mustReadDismissed) "已了解" else "我知道了", fontSize = 16.sp)
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
