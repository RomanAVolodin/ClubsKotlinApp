package com.example.cubswaitressapp.Models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Hall (
    val id: Int,
    val name: String
): Parcelable

data class AllHalls (
    val halls_list: List<Hall>
)