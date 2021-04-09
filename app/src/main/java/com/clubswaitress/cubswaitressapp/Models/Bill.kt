package com.clubswaitress.cubswaitressapp.Models

import java.io.Serializable


data class Bill (
    val id: Int,
    val isPrinted: Boolean,
    val guests: Int,
    val isGuestsNeed: Boolean,
    val isOpened: Boolean,
    val editingHardwareID: Int,
    val hall_title: String,
    val hall_id: Int,
    val number: String,
    val table_number: Int,
    val table_id: Int,
    val total: String,
    val total_discount: String,
    val total_payment: String,
    val personalName: String,
    val personal_id: Int,
    val clientsName: String,
    val clientsGroup: String,
    val gclients_total_fix_discount: String,
    val gclients_total_discount: String,
    val opened: String,
    val orders: List<Order>
): Serializable {
    public fun isNeededMissed(): Boolean {
        return this.orders.indexOfFirst { it.isNeededMissed } != -1
    }
}