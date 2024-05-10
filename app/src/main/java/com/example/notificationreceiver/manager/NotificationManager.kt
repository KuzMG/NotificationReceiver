package com.example.notificationreceiver.manager

import android.util.Log
import com.example.notificationreceiver.TAG
import com.example.notificationreceiver.data.Telemetry
import com.example.notificationreceiver.data.Webhook
import com.squareup.okhttp.Call
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import org.json.JSONObject

private const val URL = "https://royalfinance.org"

class NotificationManager(private val token: String) {
    private val client = OkHttpClient()
    fun sendTelemetry(telemetry: Telemetry): Call {
        Log.i(TAG,"Отправка $telemetry")
        val jsonData = JSONObject()
        jsonData
            .accumulate("android_id", telemetry.androidId)
            .accumulate("app_version", telemetry.appVersion)
            .accumulate("device", telemetry.device)
            .accumulate("slot_number_first", telemetry.slotNumberFirst)
            .accumulate("slot_number_second", telemetry.slotNumberSecond)
            .accumulate("battery", telemetry.battery)
            .accumulate("fingerprint", telemetry.fingerprint)

        val requestBody = RequestBody.create(
            MediaType.parse("application/json"),
            jsonData.toString()
        )
        val request = Request.Builder()
            .header("Authorization", "Token $token")
            .url("$URL/api/v2/telemetries/")
            .post(requestBody)
            .build()
        return client.newCall(request)
    }

    fun sendWebHook(webhook: Webhook): Call {
        Log.i(TAG,"Отправка $webhook")
        val jsonData = JSONObject()
        jsonData
            .accumulate("android_id", webhook.androidId)
            .accumulate("title", webhook.title)
            .accumulate("text", webhook.text)
            .accumulate("received_at", webhook.receivedAt)
            .accumulate("slot_number_first", webhook.slotNumberFirst)
            .accumulate("slot_number_second", webhook.slotNumberSecond)
            .accumulate("package_name", webhook.packageName)

        val requestBody = RequestBody.create(
            MediaType.parse("application/json"),
            jsonData.toString()
        )
        val request = Request.Builder()
            .header("Authorization", "Token $token")
            .url("$URL/api/v2/webhook/")
            .post(requestBody)
            .build()
        return client.newCall(request)
    }


}