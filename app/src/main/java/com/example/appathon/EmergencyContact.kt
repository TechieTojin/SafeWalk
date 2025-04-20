package com.example.appathon

/**
 * Data class representing an emergency contact
 */
data class EmergencyContact(
    val id: Long = 0,
    val name: String,
    val phone: String,
    val userId: Long,
    val relationship: String = ""
) 