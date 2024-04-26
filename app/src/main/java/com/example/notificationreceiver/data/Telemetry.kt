package com.example.notificationreceiver.data

data class Telemetry(
    val androidId: String,
    val appVersion: String,
    val device: String,
    val slotNumberFirst: String,
    val slotNumberSecond: String,
    val battery: String,
    val fingerprint: String = "-"
)
