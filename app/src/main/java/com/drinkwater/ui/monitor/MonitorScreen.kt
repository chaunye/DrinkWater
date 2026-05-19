package com.drinkwater.ui.monitor

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.rememberAsyncImagePainter
import com.drinkwater.DrinkWaterApp
import com.drinkwater.data.model.MonitoredApp
import com.drinkwater.data.model.PopupMode
import com.drinkwater.data.model.Reminder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as DrinkWaterApp).database
    val scope = rememberCoroutineScope()

    val monitoredApps by db.monitoredAppDao().getAll().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<MonitoredApp?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("应用监控") },
            actions = {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }
        )

        if (monitoredApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("还没有监控任何应用", color = Color.Gray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("点击右上角 + 添加要监控的应用", color = Color.LightGray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(monitoredApps, key = { it.packageName }) { app ->
                    MonitoredAppCard(
                        app = app,
                        onToggle = {
                            scope.launch {
                                db.monitoredAppDao().update(app.copy(isEnabled = !app.isEnabled))
                            }
                        },
                        onClick = { selectedApp = app },
                        onDelete = {
                            scope.launch {
                                db.monitoredAppDao().delete(app)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddAppDialog(
            onDismiss = { showAddDialog = false },
            onAppSelected = { packageName, appName ->
                scope.launch {
                    val existing = db.monitoredAppDao().getByPackageName(packageName)
                    if (existing == null) {
                        db.monitoredAppDao().insert(
                            MonitoredApp(
                                packageName = packageName,
                                appName = appName
                            )
                        )
                        // Add default reminder
                        db.reminderDao().insert(
                            Reminder(appPackageName = packageName, content = "你今天喝水了吗？")
                        )
                    }
                }
                showAddDialog = false
            }
        )
    }

    selectedApp?.let { app ->
        AppConfigDialog(
            app = app,
            onDismiss = { selectedApp = null }
        )
    }
}

@Composable
fun MonitoredAppCard(
    app: MonitoredApp,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            animationSpec = tween(300),
            initialOffsetY = { it / 4 }
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon.toBitmap(48, 48).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.appName, fontWeight = FontWeight.Medium)
                    Text(
                        if (app.isAllDay) "全天候监控" else "${app.startTime} - ${app.endTime}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Switch(checked = app.isEnabled, onCheckedChange = { onToggle() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppDialog(onDismiss: () -> Unit, onAppSelected: (String, String) -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    var apps by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .sortedBy { pm.getApplicationLabel(it).toString() }
            .map { it.packageName to pm.getApplicationLabel(it).toString() }
        isLoading = false
    }

    var searchQuery by remember { mutableStateOf("") }
    val filtered = apps.filter { (pkg, name) ->
        searchQuery.isBlank() || name.contains(searchQuery, ignoreCase = true) || pkg.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择要监控的应用 (${apps.size})") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索应用...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(filtered, key = { it.first }) { (pkg, name) ->
                            val icon = remember(pkg) {
                                try { pm.getApplicationIcon(pkg) } catch (e: Exception) { null }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAppSelected(pkg, name) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (icon != null) {
                                    Image(
                                        bitmap = icon.toBitmap(36, 36).asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column {
                                    Text(name)
                                    Text(pkg, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun AppConfigDialog(app: MonitoredApp, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val db = (context.applicationContext as DrinkWaterApp).database
    val scope = rememberCoroutineScope()

    val reminders by db.reminderDao().getByApp(app.packageName).collectAsState(initial = emptyList())
    var newReminderText by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(app.isAllDay) }
    var startTime by remember { mutableStateOf(app.startTime) }
    var endTime by remember { mutableStateOf(app.endTime) }
    var popupImageUri by remember { mutableStateOf(app.popupImageUri) }
    var backgroundImageUri by remember { mutableStateOf(app.backgroundImageUri) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            popupImageUri = it.toString()
            scope.launch {
                db.monitoredAppDao().update(app.copy(popupImageUri = it.toString()))
            }
        }
    }

    val bgImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            backgroundImageUri = it.toString()
            scope.launch {
                db.monitoredAppDao().update(app.copy(backgroundImageUri = it.toString()))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${app.appName} - 监控配置") },
        text = {
            Column {
                Text("提醒文案", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                reminders.forEach { reminder ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(reminder.content, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        IconButton(onClick = {
                            scope.launch { db.reminderDao().delete(reminder) }
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "删除", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newReminderText,
                        onValueChange = { newReminderText = it },
                        placeholder = { Text("添加新提醒...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (newReminderText.isNotBlank()) {
                            scope.launch {
                                db.reminderDao().insert(Reminder(appPackageName = app.packageName, content = newReminderText.trim()))
                            }
                            newReminderText = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("自定义图片", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Popup image
                    Column(modifier = Modifier.weight(1f)) {
                        Text("弹窗图片", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (popupImageUri != null) {
                            Box(modifier = Modifier.height(80.dp).fillMaxWidth()) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = popupImageUri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = {
                                        popupImageUri = null
                                        scope.launch {
                                            db.monitoredAppDao().update(app.copy(popupImageUri = null))
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "删除", modifier = Modifier.size(16.dp), tint = Color.White)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { imagePicker.launch("image/*") },
                                modifier = Modifier.fillMaxWidth().height(80.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("选择图片", fontSize = 12.sp)
                            }
                        }
                    }

                    // Background image
                    Column(modifier = Modifier.weight(1f)) {
                        Text("背景图片", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (backgroundImageUri != null) {
                            Box(modifier = Modifier.height(80.dp).fillMaxWidth()) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = backgroundImageUri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = {
                                        backgroundImageUri = null
                                        scope.launch {
                                            db.monitoredAppDao().update(app.copy(backgroundImageUri = null))
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "删除", modifier = Modifier.size(16.dp), tint = Color.White)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { bgImagePicker.launch("image/*") },
                                modifier = Modifier.fillMaxWidth().height(80.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("选择图片", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("生效时段", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Switch(checked = isAllDay, onCheckedChange = {
                        isAllDay = it
                        scope.launch {
                            db.monitoredAppDao().update(app.copy(isAllDay = it, startTime = startTime, endTime = endTime))
                        }
                    })
                    Text(if (isAllDay) "全天" else "自定义", fontSize = 12.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}
