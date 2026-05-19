package com.drinkwater.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Check if accessibility service is enabled but not running
        if (!AppMonitorService.isRunning() && isAccessibilityServiceEnabled(context)) {
            // The system will restart the accessibility service automatically
            // We just need to ensure the service component is enabled
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val serviceName = "${context.packageName}/.service.AppMonitorService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(serviceName)
    }
}
