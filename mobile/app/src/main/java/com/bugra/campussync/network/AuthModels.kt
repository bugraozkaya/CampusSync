package com.bugra.campussync.network

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val access: String,
    val refresh: String,
    val role: String,
    val username: String,
    @SerializedName("must_change_password") val mustChangePassword: Boolean = false,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val title: String? = null
)
