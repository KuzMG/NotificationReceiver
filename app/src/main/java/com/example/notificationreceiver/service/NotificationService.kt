package com.example.notificationreceiver.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.notificationreceiver.APP_VERSION
import com.example.notificationreceiver.MainActivity
import com.example.notificationreceiver.NOTIFICATION_CHANNEL_ID
import com.example.notificationreceiver.PHONE1_PREF
import com.example.notificationreceiver.PHONE2_PREF
import com.example.notificationreceiver.R
import com.example.notificationreceiver.SETTINGS
import com.example.notificationreceiver.TOKEN_PREF
import com.example.notificationreceiver.data.Telemetry
import com.example.notificationreceiver.data.Webhook
import com.example.notificationreceiver.manager.NotificationManager
import com.squareup.okhttp.Callback
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import java.io.IOException
import java.util.Date
import java.util.Timer
import java.util.TimerTask


val MESSAGE_EXTRA = "message"
const val DELIVERED = "SMS_DELIVERED"
private const val NOTIFICATION_ID = 2
private const val PERIOD: Long = 1 * 60 * 1000

class NotificationService : NotificationListenerService() {
    private val versionMap = mapOf(
        24 to "Android 7.0",
        25 to "Android 7.1",
        26 to "Android 8.0",
        27 to "Android 8.1",
        28 to "Android 9",
        29 to "Android 10",
        30 to "Android 11",
        31 to "Android 12",
        32 to "Android 12",
        33 to "Android 13",
        34 to "Android 14"
    )
    private lateinit var settings: SharedPreferences
    lateinit var manager: NotificationManager
    lateinit var phone1: String
    lateinit var phone2: String
    lateinit var androidId: String
    private var timer: Timer? = Timer()

    override fun onCreate() {
        super.onCreate()
        androidId = Settings.Secure.getString(
            getApplicationContext().getContentResolver(),
            Settings.Secure.ANDROID_ID
        )
        settings = getSharedPreferences(SETTINGS, MODE_PRIVATE)
        phone1 = settings.getString(PHONE1_PREF, "")!!
        phone2 = settings.getString(PHONE2_PREF, "")!!
        val token = settings.getString(TOKEN_PREF, "")!!
        manager = NotificationManager(token)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText(getString(R.string.notification_title_not))
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        timer?.schedule(object : TimerTask() {
            override fun run() {
                try {
                    val batteryIntent =
                        registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val level = batteryIntent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val sdk = Build.VERSION.SDK_INT
                    val telemetry = Telemetry(
                        androidId,
                        APP_VERSION,
                        versionMap[sdk] ?: "API level $sdk",
                        phone1,
                        phone2,
                        level.toString()
                    )

                    val code = manager.sendTelemetry(telemetry).execute().code()
                    if (code != 200) {
                        exceptionBroadcast(Exception("Ошибка отправки телеметрии на сервер. Код: $code").toString())
                    } else {
                        exceptionBroadcast("Телеметрия отправлена")
                    }
                } catch (e: Exception) {
                    exceptionBroadcast(e.toString())
                }
            }

        }, 0, PERIOD)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null && intent.action != null && intent.action!!.isNotEmpty()) {
            tryReconnectService()
        }


        return START_STICKY
    }


    fun tryReconnectService() {
        try {
            toggleNotificationListenerService()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val componentName = ComponentName(
                    applicationContext,
                    NotificationService::class.java
                )
                requestRebind(componentName)
            }
        } catch (e: Exception) {
            exceptionBroadcast(e.toString())
        }

    }

    private fun toggleNotificationListenerService() {
        try {
            val pm = packageManager
            pm.setComponentEnabledSetting(
                ComponentName(this, NotificationService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                ComponentName(this, NotificationService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            exceptionBroadcast(e.toString())
        }

    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pack = sbn.packageName
//        Log.i("Push", "Уведомление $pack")
        if (pack != "ru.raiffeisennews")
            return
        val extras = sbn.notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)
        var applicationName = getAppNameFromPkgName(this, pack)

        sendWebHook(applicationName, "$title $text")
        cancelAllNotifications()
    }

    fun sendWebHook(name: String, text: String) {
        val date = Math.floor((Date().time / 1000).toDouble())
        val webhook = Webhook(androidId, name, text, date, phone1, phone2)
        manager.sendWebHook(webhook).enqueue(object : Callback {
            override fun onFailure(request: Request, e: IOException) {
                exceptionBroadcast(e.toString())
            }

            override fun onResponse(response: Response) {
                if (response.code() != 200) {
                    exceptionBroadcast(Exception("Ошибка отправки уведомления на сервер. Код: ${response.code()}").toString())
                } else {
                    exceptionBroadcast("Уведомление отправлен")
                }
            }
        })
    }

    fun exceptionBroadcast(message: String) {
        sendBroadcast(Intent(DELIVERED).apply {
            putExtra(MESSAGE_EXTRA, message)
        })
    }

    fun getAppNameFromPkgName(context: Context, Packagename: String?): String {
        return try {
            val packageManager = context.packageManager
            val info =
                packageManager.getApplicationInfo(Packagename!!, PackageManager.GET_META_DATA)
            packageManager.getApplicationLabel(info) as String
        } catch (e: PackageManager.NameNotFoundException) {
            "Untitled"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        timer = null
    }


}