package com.example.notificationreceiver.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.notificationreceiver.APP_VERSION
import com.example.notificationreceiver.PHONE1_PREF
import com.example.notificationreceiver.PHONE2_PREF
import com.example.notificationreceiver.R
import com.example.notificationreceiver.SETTINGS
import com.example.notificationreceiver.TOKEN_PREF
import com.example.notificationreceiver.databinding.FragmentDetailBinding
import com.example.notificationreceiver.getIntentForNotificationAccess
import com.example.notificationreceiver.isMyServiceRunning
import com.example.notificationreceiver.registerReceiverCompat
import com.example.notificationreceiver.service.DELIVERED
import com.example.notificationreceiver.service.NotificationService
import com.example.notificationreceiver.service.STATUS_EXTRA


class DetailFragment : Fragment() {

    private lateinit var settings: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var binding: FragmentDetailBinding
    private lateinit var viewModel: DetailViewModel


    //    private val requestPermission =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
//            if (isGranted) {
//                check()
//            }
//        }
    private val requestPermissionNotificationActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, arg1: Intent) {
            val status = arg1.getBooleanExtra(STATUS_EXTRA, false)
            when (resultCode) {
                Activity.RESULT_OK -> {
                    viewServiceStatus(status)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetailBinding.inflate(inflater)
        settings = requireContext().getSharedPreferences(SETTINGS, AppCompatActivity.MODE_PRIVATE)
        editor = settings.edit()
        binding.run {
            appVersionTextView.text = getString(R.string.app_version, APP_VERSION)
            androidVersionTextView.text = getString(R.string.android_version, Build.VERSION.RELEASE)
            deviceIdTextView.text = getString(
                R.string.device_id, Settings.Secure.getString(
                    requireContext().contentResolver,
                    Settings.Secure.ANDROID_ID
                )
            )
            tokenTextView.text =
                getString(R.string.token_info, settings.getString(TOKEN_PREF, "")!!)
            phone1TextView.text =
                getString(R.string.phone1_info, settings.getString(PHONE1_PREF, "")!!)
            phone2TextView.text =
                getString(R.string.phone2_info, settings.getString(PHONE2_PREF, "")!!)

            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
            toolbar.setNavigationIcon(R.drawable.ic_back)

        }
        viewServiceStatus(requireContext().isMyServiceRunning(NotificationService::class.java))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[DetailViewModel::class.java]
        viewModel.pingLiveData.observe(viewLifecycleOwner) {
            binding.pingTextView.text = getString(R.string.ping, it.toString())
        }
        viewModel.logCat {
            binding.logTextView.append(it)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireContext().registerReceiverCompat(receiver, IntentFilter(DELIVERED))
    }


    fun viewServiceStatus(status: Boolean) {
        if (status) {
            binding.statusServiceCardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.green
                )
            )
            binding.statusServiceTextView.text = getString(R.string.service_up)
        } else {
            binding.statusServiceCardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.red
                )
            )
            binding.statusServiceTextView.text = getString(R.string.service_down)
        }
    }


    override fun onStart() {
        super.onStart()
        binding.run {
            permissionButton.setOnClickListener {
                val pm: PowerManager =
                    getSystemService(requireContext(), PowerManager::class.java) as PowerManager
                if (!pm.isIgnoringBatteryOptimizations(requireContext().packageName)) {
                    Toast.makeText(
                        requireContext(),
                        "Нажмите на battery restrisions и разрешите работу в фоне",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } else {
                    if (requireContext().isMyServiceRunning(NotificationService::class.java)) {
                        requireContext().stopService(
                            Intent(
                                requireContext(),
                                NotificationService::class.java
                            )
                        )
                    } else {
                        startForegroundService(
                            requireContext(),
                            Intent(requireContext(), NotificationService::class.java)
                        )
                    }
                    requestPermissionNotificationActivity.launch(
                        getIntentForNotificationAccess(
                            requireContext().packageName,
                            NotificationService::class.java
                        )
                    )
                }
            }
            batteryButton.setOnClickListener {
                val intent = Intent()
                val pm: PowerManager =
                    getSystemService(requireContext(), PowerManager::class.java) as PowerManager
                if (!pm.isIgnoringBatteryOptimizations(requireContext().packageName)) {
                    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    intent.data = Uri.parse("package:${requireContext().packageName}")
                    startActivity(intent)
                }
            }
            toolbar.setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
            shareLogButton.setOnClickListener {
                val text = logTextView.text.toString()
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }

                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            }
            clearLogButton.setOnClickListener {
                viewModel.clearLog {
                    logTextView.text = ""
                }
            }
        }


    }


    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterReceiver(receiver)
    }

}