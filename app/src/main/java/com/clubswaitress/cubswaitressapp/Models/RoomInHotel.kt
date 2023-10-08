package com.clubswaitress.cubswaitressapp.Models

import java.io.Serializable
import java.util.*

data class RoomInHotel (
    val id: Int,
    val ocupationStatus: Int,
    var room: Room,
    val startedAt: String?,
    val closedAt: String?,
    val durationMinutes: String?
): Serializable

data class Room (
    val id: Int,
    val roomNumber: String,
    val order: Int
): Serializable

data class RecordedActionsOrderedByRoom (
    val recordedActionsOrderedByRoom: List<RoomInHotel>
): Serializable