package com.clubswaitress.cubswaitressapp.Models

import java.io.Serializable

data class Client (
    val id: Int,
    val full_name: String,
    val phone: String,
    val card: String,
    val blocked: Boolean,
    val note: String,
    val dateadd: String,
    val last_date: String,
    val cli_sum: Double,
    val group_name: String,
    var qr_code: String
): Serializable
