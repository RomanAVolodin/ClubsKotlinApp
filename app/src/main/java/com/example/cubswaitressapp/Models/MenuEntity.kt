package com.example.cubswaitressapp.Models

data class MenuEntity (
    val id: Int,
    val name_button: String,
    val basePrice: Int,
    val actualPriceInHall: Int,
    val additionsOne: List<MenuEntityAddition>,
    val additionsNo: List<MenuEntityAddition>,
    val additionsTime: List<MenuEntityAddition>,
    val additionsNeed: List<MenuEntityAddition>,
    var amount: Double
)

data class MenuEntityAddition(
    val id: Int,
    val name_button: String,
    var isSelected: Boolean = false
)