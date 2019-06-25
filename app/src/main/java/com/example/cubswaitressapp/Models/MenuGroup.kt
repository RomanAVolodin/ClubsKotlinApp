package com.example.cubswaitressapp.Models

data class MenuGroup (
    val id: Int,
    val name: String,
    val isSubmenus: Boolean,
    val parent_id: Int
)