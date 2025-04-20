package com.example.appathon

/**
 * Data class representing a safe zone location like police stations, hospitals, etc.
 */
data class SafeZone(
    val id: Int,
    val name: String,
    val type: String, // "POLICE", "HOSPITAL", "FIRE_STATION", etc.
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val contactNumber: String? = null,
    val operationHours: String? = null,
    val iconResId: Int // Resource ID for the marker icon
) 