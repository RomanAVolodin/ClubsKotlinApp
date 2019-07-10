package com.example.cubswaitressapp.Models

data class Order (
    val id: Int,
    val menu_item_id: Int,
    val menu_item: String,
    val isPrinted: Boolean,
    val qnt: Double,
    val price: String,
    val childs: List<OrderChild>,
    val menuEntity: MenuEntity
)

data class OrderChild(
    val id: Int,
    val title: String,
    val prefix_title: String,
    val type_id: Int
)