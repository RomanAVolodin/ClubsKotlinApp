package com.clubswaitress.cubswaitressapp.Models

import java.io.Serializable

data class Order (
    val id: Int,
    val menu_item_id: Int,
    val menu_item: String,
    val isPrinted: Boolean,
    var qnt: Double,
    val price: String,
    val price_num: String,
    val price_discount: String,
    val price_discount_text: String,
    val price_discount_percentage: String,
    val price_fix: String,
    var isReadyForTransfer: Boolean = false,
    val childs: List<OrderChild>,
    var menuEntity: MenuEntity,
    var cancel_order_id: Int,
    var input_time: String,
    var isNeededMissed: Boolean = false
): Serializable

data class OrderChild(
    val id: Int,
    val title: String,
    val prefix_title: String,
    val type_id: Int
): Serializable