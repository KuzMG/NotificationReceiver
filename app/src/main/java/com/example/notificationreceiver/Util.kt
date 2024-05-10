package com.example.notificationreceiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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

fun getAppNameFromPkgName(context: Context, packageName: String): String {
    return try {
        val packageManager = context.packageManager
        val info =
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        packageManager.getApplicationLabel(info) as String
    } catch (e: PackageManager.NameNotFoundException) {
        "Untitled"
    }
}
