package com.example.cubswaitressapp.Models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

class Table (
    val id: Int,
    val number: Int,
    val total: String,
    val bills: List<TableBill>
)

@Parcelize
data class TableBill (
    val id: Int,
    val isPrinted: Boolean,
    val isOpened: Boolean,
    val editingHardwareID: Int,
    val number: Int,
    val total: String?,
    val personalName: String?,
    val clientsName: String?,
    val total_discount: String?,
    val total_payment: String?,
    val opened: String?
): Parcelable