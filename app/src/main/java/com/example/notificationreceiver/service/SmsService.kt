package com.example.notificationreceiver.service

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.provider.Settings
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.example.notificationreceiver.MainActivity
import com.example.notificationreceiver.NOTIFICATION_CHANNEL_ID
import com.example.notificationreceiver.PHONE1_PREF
import com.example.notificationreceiver.PHONE2_PREF
import com.example.notificationreceiver.R
import com.example.notificationreceiver.SETTINGS
import com.example.notificationreceiver.TOKEN_PREF
import com.example.notificationreceiver.data.Webhook
import com.example.notificationreceiver.manager.NotificationManager
import com.example.notificationreceiver.registerReceiverCompat
import com.example.notificationreceiver.startForegroundService
import com.squareup.okhttp.Callback
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import java.io.IOException
import java.util.Date

private const val NOTIFICATION_ID = 1

class SmsService : Service() {

    lateinit var manager: NotificationManager
    lateinit var phone1: String
    lateinit var phone2: String
    private var flagReceiver = false
    lateinit var androidId: String
    private val smsSender = arrayOf("900", "Raiffeisen", "Tinkoff","Alfa-Bank")
    private lateinit var settings: SharedPreferences

    private val receiverSms = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, arg1: Intent) {
            val msg = StringBuilder()
            var name = ""
            var flag = false
            for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(arg1)) {
                if (smsSender.contains(smsMessage.originatingAddress)) {
                    msg.append(smsMessage.messageBody)
                    name = smsMessage.originatingAddress!!
                    flag = true
                }
            }

            if (flag) {
                sendWebHook(name, msg.toString())
            }

        }
    }

    override fun onCreate() {
        super.onCreate()
        androidId = Settings.Secure.getString(
            getApplicationContext().getContentResolver(),
            Settings.Secure.ANDROID_ID
        )
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText(getString(R.string.notification_title_sms))
            .setContentIntent(pendingIntent)
            .build()
        startForegroundService(this, NOTIFICATION_ID, notification)

        settings = getSharedPreferences(SETTINGS, MODE_PRIVATE)
        phone1 = settings.getString(PHONE1_PREF, "")!!
        phone2 = settings.getString(PHONE2_PREF, "")!!
        val token = settings.getString(TOKEN_PREF, "")!!
        manager = NotificationManager(token)
        registerReceiverCompat(
            receiverSms,
            IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        )
        flagReceiver = true
        return START_STICKY
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
                    exceptionBroadcast(Exception("Ошибка отправки сообщения на сервер. Код: ${response.code()}").toString())
                } else {
                    exceptionBroadcast("Сообщение отправлен")
                }
            }

        })
    }

    fun exceptionBroadcast(message: String) {
        sendBroadcast(Intent(DELIVERED).apply {
            putExtra(MESSAGE_EXTRA, message)
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        if (flagReceiver)
            unregisterReceiver(receiverSms)
    }


    override fun onBind(intent: Intent?) = null
}