package com.example.cubswaitressapp.Models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Hall (
    val id: Int,
    val name: String,
    val show_tables_total: Boolean,
    val delete_empty_bill: Boolean

): Parcelable

data class AllHalls (
    val halls_list: List<Hall>
)