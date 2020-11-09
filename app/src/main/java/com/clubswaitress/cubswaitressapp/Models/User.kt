package com.clubswaitress.cubswaitressapp.Models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class User (
    val id: Int,
    val username: String,
    val dolzhnost_name: String,
    val hardware_name: String,
    val safe_actions: List<SafeAction>
): Parcelable {

    public fun isActionAllowed(actionId: Int): Boolean {
        for (act in this.safe_actions) {
            if (act.id == actionId) {
                return true
            }
        }
        return false
    }
}

@Parcelize
data class SafeAction (
    val id: Int,
    val name: String?
): Parcelable