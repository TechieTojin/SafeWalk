package com.example.appathon

data class User(
    val id: String,
    val username: String,
    val email: String,
    val phoneNumber: String = ""
) 