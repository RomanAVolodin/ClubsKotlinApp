package com.clubswaitress.cubswaitressapp.Models

import java.io.Serializable

data class MenuEntity (
    val id: Int,
    val name_button: String,
    val basePrice: Int,
    val weight: Int,
    val actualPriceInHall: Int,
    val deleted: Boolean,
    val additions: List<MenuAdditionType>,
    var amount: Double
): Serializable

data class MenuAdditionType (
    val id: Int,
    val isNeed: Boolean,
    var isSelected: Boolean = false,
    val name: String,
    val name_button: String,
    val items: List<MenuEntityAddition>
): Serializable

data class MenuEntityAddition(
    val id: Int,
    val name: String,
    var isSelected: Boolean = false
): Serializable