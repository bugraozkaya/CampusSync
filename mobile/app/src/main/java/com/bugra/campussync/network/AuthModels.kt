package com.bugra.campussync.network // Paket adına dikkat et

// Django'ya göndereceğimiz giriş bilgileri
data class LoginRequest(
    val username: String,
    val password: String
)

// Django'dan bize dönecek olan Token bilgileri
data class LoginResponse(
    val access: String,
    val refresh: String
)