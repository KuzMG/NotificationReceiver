package com.example.notificationreceiver.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.util.Log
import com.example.notificationreceiver.PHONE1_PREF
import com.example.notificationreceiver.PHONE2_PREF
import com.example.notificationreceiver.SETTINGS
import com.example.notificationreceiver.TAG
import com.example.notificationreceiver.TOKEN_PREF
import com.example.notificationreceiver.data.Webhook
import com.example.notificationreceiver.manager.NotificationManager
import com.squareup.okhttp.Callback
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import java.io.IOException
import java.util.Date
import kotlin.math.floor

class SmsReceiver : BroadcastReceiver() {
    private var settings: SharedPreferences? = null
    private val smsSender = arrayOf("900", "Raiffeisen", "Tinkoff", "Alfa-Bank", "Rosbank", "MBank", "URALSIB")
    private var manager: NotificationManager?  = null

    override fun onReceive(context: Context, intent: Intent) {
        if(settings==null){
            settings = context.getSharedPreferences(SETTINGS, NotificationListenerService.MODE_PRIVATE)
        }
        if(manager ==  null){
            val token = settings!!.getString(TOKEN_PREF, "")!!
            manager = NotificationManager(token)
        }
        val msg = StringBuilder()
        var name = ""
        var flag = false
        for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            if (smsSender.contains(smsMessage.originatingAddress)) {
                msg.append(smsMessage.messageBody)
                name = smsMessage.originatingAddress!!
                flag = true
            }
        }
        if (flag) {
            Log.i(TAG,"Получено сообщение от $name")
            sendWebHook(context,name, msg.toString())
        } else{
            Log.i(TAG,"Полученое сообщение не прошло фильтр")
        }
    }

    fun sendWebHook(context: Context,name: String, text: String) {
        val phone1 = settings!!.getString(PHONE1_PREF, "")!!
        val phone2 = settings!!.getString(PHONE2_PREF, "")!!
        val date = floor((Date().time / 1000).toDouble())
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        val webhook = Webhook(androidId, name, text, date, phone1, phone2)
        manager!!.sendWebHook(webhook).enqueue(object : Callback {
            override fun onFailure(request: Request, e: IOException) {
                Log.i(TAG,"Ошибка отправки $webhook на сервер. $e")
            }

            override fun onResponse(response: Response) {

                if (response.code() != 200) {
                    response.request().body()
                    Log.i(TAG,"Ошибка отправки $webhook на сервер. ${response.body().string()}")

                } else {
                    Log.i(TAG,"$webhook отправлен")
                }
            }
        })
    }
}