package com.example.cubswaitressapp.Models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class User (val id: Int, val username: String): Parcelable {
    constructor(): this(0, "")
}