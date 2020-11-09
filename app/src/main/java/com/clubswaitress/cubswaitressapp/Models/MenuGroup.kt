package com.clubswaitress.cubswaitressapp.Models

data class MenuGroup (
    val id: Int,
    val name: String,
    val isSubmenus: Boolean,
    val parent_id: Int,
    val isHotkey: Boolean = false
)