package com.example.notificationreceiver

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.notificationreceiver.service.DELIVERED
import com.example.notificationreceiver.service.MESSAGE_EXTRA
import com.example.notificationreceiver.service.NotificationService
import com.example.notificationreceiver.service.SmsService
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Timer
import java.util.TimerTask


const val SETTINGS = "settings"
const val SERVICE_PREF = "service"
const val PHONE1_PREF = "phone1"
const val PHONE2_PREF = "phone2"
const val TOKEN_PREF = "token"

class MainActivity : AppCompatActivity() {

    private lateinit var settings: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var settingsButton: Button
    private lateinit var phone1EditText: EditText
    private lateinit var phone2EditText: EditText
    private lateinit var infoTextView: TextView
    private lateinit var tokenEditText: EditText
    private lateinit var telemetryTextView: TextView
    private lateinit var qrScanButton: Button
    private lateinit var infoCardView: CardView
    private lateinit var pingTextView: TextView
    private var timerPing: Timer? = null
    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                check()
            }
        }
    private val requestPermissionNotificationActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    private val scanQrResultLauncher =
        registerForActivityResult(ScanContract(), { result ->
            if (result.getContents() != null) {
                tokenEditText.setText(result.contents)
            }
        })

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, arg1: Intent) {
            val message = arg1.getStringExtra(MESSAGE_EXTRA)
            when {
                resultCode == Activity.RESULT_OK -> {
                    infoTextView.setText(message)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settings = getSharedPreferences(SETTINGS, MODE_PRIVATE)
        editor = settings.edit()
        startButton = findViewById(R.id.start_button)
        stopButton = findViewById(R.id.stop_button)
        settingsButton = findViewById(R.id.settings_button)
        phone1EditText = findViewById(R.id.phone1_edit_text)
        phone2EditText = findViewById(R.id.phone2_edit_text)
        infoTextView = findViewById(R.id.info_text_view)
        tokenEditText = findViewById(R.id.token_edit_text)
        qrScanButton = findViewById(R.id.qr_scan_button)
        telemetryTextView = findViewById(R.id.telemetry_text_view)
        infoCardView = findViewById(R.id.info_card_view)
        pingTextView = findViewById(R.id.ping_text_view)
        startButton.isEnabled = !settings.getBoolean(SERVICE_PREF, false)
        stopButton.isEnabled = settings.getBoolean(SERVICE_PREF, false)
        settingsButton.isEnabled = settings.getBoolean(SERVICE_PREF, false)
        val androidId = Settings.Secure.getString(
            getApplicationContext().getContentResolver(),
            Settings.Secure.ANDROID_ID
        )
        telemetryTextView.setText(
            "Phone ID: $androidId\n" +
                    "App version: $APP_VERSION"
        )
    }

    override fun onStart() {
        super.onStart()
        registerReceiverCompat(receiver, IntentFilter(DELIVERED))
        infoCardView.setOnClickListener {
            if (infoTextView.text.isNotEmpty()) {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("", infoTextView.text)
                clipboard.setPrimaryClip(clip)
            }
        }
        qrScanButton.setOnClickListener {
            val scanOptions = ScanOptions()
            scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            scanOptions.setPrompt("Отсканируйте QR-code")
            scanOptions.setBeepEnabled(false)
            scanQrResultLauncher.launch(scanOptions)
        }
        startButton.setOnClickListener {
            check()
        }
        stopButton.setOnClickListener {
            var flag = true
            val am = this.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val rs = am.getRunningServices(50)
            for (i in rs.indices) {
                val rsi = rs[i]
                if (rsi.service.className == "com.example.notificationreceiver.service.NotificationService") {
                    flag = false
                }
            }
            if (flag) {
                stopService(Intent(this, SmsService::class.java))
                startButton.isEnabled = true
                stopButton.isEnabled = false
                settingsButton.isEnabled = false
                editor.putBoolean(SERVICE_PREF, false).apply()
            }
        }
        settingsButton.setOnClickListener {
            requestPermissionNotificationActivity.launch(
                getIntentForNotificationAccess(
                    packageName,
                    NotificationService::class.java
                )
            )

        }

        timerPing = Timer()
        timerPing?.schedule(object : TimerTask() {
            override fun run() {
                val ping = getPing()
                runOnUiThread {
                    pingTextView.setText(getString(R.string.ping, ping.toString()))
                }
            }

        }, 0, 3000)
    }

    private fun getPing(): Double {
        var res = -1.0
        try {
            val p = Runtime.getRuntime().exec("ping -c 3 royalfinance.org")
            val stdInput = BufferedReader(InputStreamReader(p.getInputStream()))
            stdInput.use {
                val tmp = it.readLines().last()
                res = tmp.substring(23, tmp.length - 3).split("/")[1].toDouble()
            }
            return res
        } catch (e: Exception) {
            return res
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(receiver)
        timerPing?.cancel()
        timerPing = null
    }

    fun check() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(Manifest.permission.RECEIVE_SMS)
        } else {
            editor.putString(PHONE1_PREF, phone1EditText.text.toString()).apply()
            editor.putString(PHONE2_PREF, phone2EditText.text.toString()).apply()
            editor.putString(TOKEN_PREF, tokenEditText.text.toString()).apply()
            ContextCompat.startForegroundService(
                this,
                Intent(this, SmsService::class.java)
            )
            startButton.isEnabled = false
            stopButton.isEnabled = true
            settingsButton.isEnabled = true
            editor.putBoolean(SERVICE_PREF, true).apply()
        }
    }
}
