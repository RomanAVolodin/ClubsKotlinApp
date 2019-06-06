package com.example.cubswaitressapp.Models

data class Bill (
    val id: Int,
    val hall_title: String,
    val hall_id: Int,
    val number: String,
    val table_number: Int,
    val table_id: Int,
    val total: String,
    val total_discount: String,
    val total_payment: String,
    val personalName: String,
    val clientsName: String,
    val opened: String,
    val orders: List<Order>
)