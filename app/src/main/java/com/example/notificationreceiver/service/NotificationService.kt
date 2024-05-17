package com.example.notificationreceiver.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.notificationreceiver.APP_VERSION
import com.example.notificationreceiver.NOTIFICATION_CHANNEL_ID
import com.example.notificationreceiver.PHONE1_PREF
import com.example.notificationreceiver.PHONE2_PREF
import com.example.notificationreceiver.R
import com.example.notificationreceiver.SETTINGS
import com.example.notificationreceiver.TAG
import com.example.notificationreceiver.TOKEN_PREF
import com.example.notificationreceiver.data.Telemetry
import com.example.notificationreceiver.data.Webhook
import com.example.notificationreceiver.getAppNameFromPkgName
import com.example.notificationreceiver.manager.NotificationManager
import com.example.notificationreceiver.startForegroundServiceCompat
import com.example.notificationreceiver.ui.MainActivity
import com.squareup.okhttp.Callback
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import java.io.IOException
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.math.floor


const val STATUS_EXTRA = "status"
const val DELIVERED = "SMS_DELIVERED"
private const val NOTIFICATION_ID = 2
private const val PERIOD: Long = 1 * 15 * 1000

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
    private val pushSenders = arrayOf("ru.raiffeisennews", "com.maanavan.mb_kyrgyzstan")
    private lateinit var settings: SharedPreferences
    lateinit var manager: NotificationManager
    lateinit var phone1: String
    lateinit var phone2: String
    lateinit var androidId: String
    private var timer: Timer? = Timer()

//    private val receiverSms = object : BroadcastReceiver() {
//        override fun onReceive(arg0: Context, arg1: Intent) {
//            val msg = StringBuilder()
//            var name = ""
//            var flag = false
//            for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(arg1)) {
//                if (smsSender.contains(smsMessage.originatingAddress)) {
//                    msg.append(smsMessage.messageBody)
//                    name = smsMessage.originatingAddress!!
//                    flag = true
//                }
//            }
//
//            if (flag) {
//                Log.i(TAG,"Пришло сообщение")
//                sendWebHook(name, msg.toString(), false)
//            }
//
//        }
//    }

    override fun onCreate() {
        super.onCreate()
        androidId = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        settings = getSharedPreferences(SETTINGS, MODE_PRIVATE)
        phone1 = settings.getString(PHONE1_PREF, "")!!
        phone2 = settings.getString(PHONE2_PREF, "")!!
        val token = settings.getString(TOKEN_PREF, "")!!
        manager = NotificationManager(token)
        Log.i(TAG,"phone1 =$phone1 \n" +
                "phone2 = $phone2 \n" +
                "token = $token")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG,"Прослушивание уведомлений запущено")
//        registerReceiverCompat(
//            receiverSms,
//            IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
//        )
//        flagReceiver = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action != null && intent.action!!.isNotEmpty()) {
            Log.i(TAG,"Сервис перезапускается...")
            tryReconnectService()
        }
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

        startForegroundServiceCompat(NOTIFICATION_ID, notification)
        statusBroadcast(true)
        Log.i(TAG,"Сервис запущен")
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
                    val response = manager.sendTelemetry(telemetry).execute()
                    if (response.code() != 200) {
                        Log.i(TAG,"Ошибка отправки телеметрии на сервер. ${response.body().string()}")
                    } else {
                        Log.i(TAG,"Телеметрия отправлена")
                    }
                } catch (e: Exception) {
                    Log.i(TAG,"Ошибка отправки телеметрии на сервер. $e")
                }
            }

        }, 0, PERIOD)
        return START_STICKY
    }


    private fun tryReconnectService() {
        try {
            toggleNotificationListenerService()
            val componentName = ComponentName(
                applicationContext,
                NotificationService::class.java
            )
            requestRebind(componentName)
        } catch (e: Exception) {
            Log.i(TAG,"Перезапуск с ошибкой tryReconnectService $e")
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
            Log.i(TAG,"Перезапуск с ошибкой toggleNotificationListenerService $e")
        }

    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pack = sbn.packageName
        if (!pushSenders.contains(pack))
            return
        Log.i(TAG,"Пришло уведомление от $pack")
        val extras = sbn.notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)
        val applicationName = getAppNameFromPkgName(this, pack)

        sendWebHook(applicationName, "$title $text", true)
        cancelAllNotifications()
    }

    fun sendWebHook(name: String, text: String, type: Boolean) {
        val strOk: String
        val strError: String
        if (type) {
            strOk = "уведомления"
            strError = "Уведомление"
        } else {
            strOk = "сообщения"
            strError = "Сообщение"
        }
        val date = floor((Date().time / 1000).toDouble())
        val webhook = Webhook(androidId, name, text, date, phone1, phone2)

        manager.sendWebHook(webhook).enqueue(object : Callback {
            override fun onFailure(request: Request, e: IOException) {
                Log.i(TAG,"Ошибка отправки $strOk на сервер. $e")
            }

            override fun onResponse(response: Response) {

                if (response.code() != 200) {
                    response.request().body()
                    Log.i(TAG,"Ошибка отправки $strOk на сервер. Код: ${response.code()}")

                } else {
                    Log.i(TAG,"$strError отправлено")
                }
            }
        })
    }

    private fun statusBroadcast(status: Boolean) {
        sendBroadcast(Intent(DELIVERED).apply {
            putExtra(STATUS_EXTRA, status)
        })
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG,"Прослушивание уведомлений остановлено")
        stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG,"Сервис остановлен")
        stop()
    }
    private fun stop(){
        statusBroadcast(false)
        timer?.cancel()
        timer = null
//        if (flagReceiver) {
//            unregisterReceiver(receiverSms)
//            flagReceiver =false
//        }
    }

}