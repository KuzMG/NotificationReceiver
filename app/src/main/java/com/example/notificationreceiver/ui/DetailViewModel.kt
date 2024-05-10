package com.example.notificationreceiver.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Timer
import java.util.TimerTask

class DetailViewModel : ViewModel() {
    private var timerPing: Timer? = null
    private val pingMutableLiveData = MutableLiveData<Double>()
    val pingLiveData: LiveData<Double>
        get() = pingMutableLiveData

    init {
        timerPing = Timer()
        timerPing?.schedule(object : TimerTask() {
            override fun run() {
                pingMutableLiveData.postValue(getPing())
            }

        }, 0, 3000)
    }


    fun logCat(logTextView: (String) -> Unit){
        viewModelScope.launch(Dispatchers.IO) {
            Runtime.getRuntime().exec("logcat -s -v time TAG:I")
                .inputStream
                .bufferedReader()
                .useLines { lines ->
                    lines.forEach { line ->
                        runBlocking(Dispatchers.Main){
                            logTextView("$line\n\n")
                        }
                    }
                }
        }
    }
//    fun logCatOutput() = liveData(Dispatchers.IO) {
//        Runtime.getRuntime().exec("logcat -s -v time TAG:I")
//            .inputStream
//            .bufferedReader()
//            .useLines { lines ->
//                lines.forEach { line ->
//                    emit("$line\n\n")
//                }
//            }
//    }

    fun clearLog(clearTextView: () -> Unit) {
        Runtime.getRuntime().exec("logcat -c")
        clearTextView()
    }

    private fun getPing(): Double {
        var res = -1.0
        return try {
            val p = Runtime.getRuntime().exec("ping -c 3 royalfinance.org")
            val stdInput = BufferedReader(InputStreamReader(p.inputStream))
            stdInput.use {
                val tmp = it.readLines().last()
                res = tmp.substring(23, tmp.length - 3).split("/")[1].toDouble()
            }
            res
        } catch (e: Exception) {
            res
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerPing?.cancel()
        timerPing = null
    }
}