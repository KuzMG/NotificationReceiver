package com.example.notificationreceiver

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

const val SETTINGS = "settings"
const val PHONE1_PREF = "phone1"
const val PHONE2_PREF = "phone2"
const val TOKEN_PREF = "token"
const val APP_VERSION = "v2.3.4"
const val NOTIFICATION_CHANNEL_ID = "1"
const val TAG = "TAG"
class NRApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}