package com.example.notificationreceiver.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.notificationreceiver.R


class MainActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container_view, StartScreenFragment())
                .commit()

    }
}
