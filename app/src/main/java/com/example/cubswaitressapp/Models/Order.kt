package com.example.cubswaitressapp.Models

data class Order (
    val id: Int,
    val menu_item: String,
    val qnt: Double,
    val price: String,
    val childs: List<OrderChild>
)

data class OrderChild(
    val id: Int,
    val title: String
)