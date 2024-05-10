package com.example.notificationreceiver.data

import android.os.Parcel
import android.os.Parcelable

data class Webhook(
    val androidId: String,
    val title: String,
    val text: String,
    val receivedAt: Double,
    val slotNumberFirst: String,
    val slotNumberSecond: String,
    val packageName: String = "com.google.android.apps.messaging"
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readDouble(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(androidId)
        parcel.writeString(title)
        parcel.writeString(text)
        parcel.writeDouble(receivedAt)
        parcel.writeString(slotNumberFirst)
        parcel.writeString(slotNumberSecond)
        parcel.writeString(packageName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Webhook> {
        override fun createFromParcel(parcel: Parcel): Webhook {
            return Webhook(parcel)
        }

        override fun newArray(size: Int): Array<Webhook?> {
            return arrayOfNulls(size)
        }
    }

}
