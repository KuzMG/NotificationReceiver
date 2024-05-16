package com.example.notificationreceiver.ui

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.notificationreceiver.PHONE1_PREF
import com.example.notificationreceiver.PHONE2_PREF
import com.example.notificationreceiver.R
import com.example.notificationreceiver.SETTINGS
import com.example.notificationreceiver.TOKEN_PREF
import com.example.notificationreceiver.getIntentForNotificationAccess
import com.example.notificationreceiver.service.NotificationService
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions


class StartScreenFragment : Fragment() {


    private lateinit var qrScanButton: Button
    private lateinit var saveButton: Button
    private lateinit var phone1EditText: EditText
    private lateinit var phone2EditText: EditText
    private lateinit var tokenEditText: EditText
    private lateinit var settings: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var toolbar: Toolbar
    private val scanQrResultLauncher =
        registerForActivityResult(ScanContract()) { result ->
            if (result.contents != null) {
                tokenEditText.setText(result.contents)
            }
        }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                check()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_start_screen, container, false)
        toolbar = view.findViewById(R.id.toolbar)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        qrScanButton = view.findViewById(R.id.qr_scan_button)
        tokenEditText = view.findViewById(R.id.token_edit_text)
        phone1EditText = view.findViewById(R.id.phone1_edit_text)
        phone2EditText = view.findViewById(R.id.phone2_edit_text)
        saveButton = view.findViewById(R.id.save_button)
        settings = requireContext().getSharedPreferences(SETTINGS, AppCompatActivity.MODE_PRIVATE)
        editor = settings.edit()

        tokenEditText.setText(settings.getString(TOKEN_PREF,""))
        phone1EditText.setText(settings.getString(PHONE1_PREF,""))
        phone2EditText.setText(settings.getString(PHONE2_PREF,""))

        return view
    }

    override fun onStart() {
        super.onStart()
        qrScanButton.setOnClickListener {
            val scanOptions = ScanOptions()
            scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            scanOptions.setPrompt("Отсканируйте QR-code")
            scanOptions.setBeepEnabled(false)
            scanQrResultLauncher.launch(scanOptions)
        }
        saveButton.setOnClickListener {
            check()
        }
    }
    fun check() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(Manifest.permission.RECEIVE_SMS)
        } else {
            editor.putString(PHONE1_PREF, phone1EditText.text.toString()).apply()
            editor.putString(PHONE2_PREF, phone2EditText.text.toString()).apply()
            editor.putString(TOKEN_PREF, tokenEditText.text.toString()).apply()
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container_view,DetailFragment())
                .addToBackStack(null)
                .commit()
        }
    }

}