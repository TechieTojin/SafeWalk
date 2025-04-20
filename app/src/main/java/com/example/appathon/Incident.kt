package com.example.appathon

import java.util.Date

/**
 * Data class representing an incident report
 */
data class Incident(
    val id: Long = 0,
    val title: String,
    val type: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val photoPath: String?, // Local file path to photo
    val reportedBy: String, // User who reported
    val reportedAt: Date = Date(), // Timestamp of report
    val userId: Long // User ID who reported the incident
) 