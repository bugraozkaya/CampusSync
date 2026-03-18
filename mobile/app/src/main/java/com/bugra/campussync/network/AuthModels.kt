package com.bugra.campussync.network

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val access: String,
    val refresh: String,
    val role: String, // YENİ EKLENDİ
    val username: String // YENİ EKLENDİ
)
