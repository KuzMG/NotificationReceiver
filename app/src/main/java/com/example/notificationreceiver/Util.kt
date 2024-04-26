package com.example.notificationreceiver

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.appcompat.app.AppCompatActivity

fun Context.registerReceiverCompat(receiver: BroadcastReceiver, filter: IntentFilter) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        registerReceiver(
            receiver, filter,
            AppCompatActivity.RECEIVER_EXPORTED
        )
    } else {
        registerReceiver(receiver, filter)
    }
}


fun startForegroundService(context: Service, id: Int, notofication: Notification) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.startForeground(
            id,
            notofication,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    } else {
        context.startForeground(id, notofication)
    }
}

fun getIntentForNotificationAccess(
    packageName: String,
    notificationAccessServiceClass: Class<out NotificationListenerService>
): Intent =
    getIntentForNotificationAccess(packageName, notificationAccessServiceClass.name)


private fun getIntentForNotificationAccess(
    packageName: String,
    notificationAccessServiceClassName: String
): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(packageName, notificationAccessServiceClassName).flattenToString()
            )
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    val value = "$packageName/$notificationAccessServiceClassName"
    val key = ":settings:fragment_args_key"
    intent.putExtra(key, value)
    intent.putExtra(":settings:show_fragment_args", Bundle().also { it.putString(key, value) })
    return intent
}
