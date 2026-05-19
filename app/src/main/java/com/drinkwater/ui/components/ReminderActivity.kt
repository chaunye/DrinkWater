package com.drinkwater.ui.components

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.drinkwater.DrinkWaterApp
import com.drinkwater.data.model.PopupMode
import com.drinkwater.data.model.ReminderAction
import com.drinkwater.data.model.ReminderLog
import com.drinkwater.data.db.SettingsDataStore
import com.drinkwater.ui.theme.DrinkWaterTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_REMINDER_CONTENT = "reminder_content"
        const val EXTRA_POPUP_MODE = "popup_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure this activity shows on top of other apps
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )
        setTurnScreenOn(true)
        setShowWhenLocked(true)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run { finish(); return }
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "未知应用"
        val content = intent.getStringExtra(EXTRA_REMINDER_CONTENT) ?: "你今天喝水了吗？"
        val popupMode = intent.getStringExtra(EXTRA_POPUP_MODE) ?: PopupMode.DEFAULT.name

        setContent {
            DrinkWaterTheme {
                if (popupMode == PopupMode.FLOATING.name) {
                    FloatingReminderContent(
                        appName = appName,
                        content = content,
                        onConfirm = { handleAction(packageName, content, ReminderAction.CONFIRMED) },
                        onDelay = { handleAction(packageName, content, ReminderAction.DELAYED) },
                        onCancel = { handleAction(packageName, content, ReminderAction.CANCELLED) }
                    )
                } else {
                    FullScreenReminderContent(
                        appName = appName,
                        content = content,
                        onConfirm = { handleAction(packageName, content, ReminderAction.CONFIRMED) },
                        onDelay = { handleAction(packageName, content, ReminderAction.DELAYED) },
                        onCancel = { handleAction(packageName, content, ReminderAction.CANCELLED) }
                    )
                }
            }
        }
    }

    private fun handleAction(packageName: String, content: String, action: ReminderAction) {
        lifecycleScope.launch {
            val db = (application as DrinkWaterApp).database
            db.reminderLogDao().insert(
                ReminderLog(
                    appPackageName = packageName,
                    reminderContent = content,
                    action = action
                )
            )
        }
        if (action == ReminderAction.CONFIRMED) {
            // Launch the target app
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        }
        finish()
    }
}

@Composable
fun FullScreenReminderContent(
    appName: String,
    content: String,
    onConfirm: () -> Unit,
    onDelay: () -> Unit,
    onCancel: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(24.dp)
                    .clickable { /* consume click */ },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App name
                    Text(
                        text = appName,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Reminder content
                    Text(
                        text = content,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("确认打开", fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDelay,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("延迟", fontSize = 14.sp, color = Color(0xFFFF9800))
                        }
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("取消", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingReminderContent(
    appName: String,
    content: String,
    onConfirm: () -> Unit,
    onDelay: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(appName, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Text(content, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onCancel) { Text("取消", color = Color.Gray) }
                    TextButton(onClick = onDelay) { Text("延迟", color = Color(0xFFFF9800)) }
                    Button(onClick = onConfirm) { Text("确认") }
                }
            }
        }
    }
}
