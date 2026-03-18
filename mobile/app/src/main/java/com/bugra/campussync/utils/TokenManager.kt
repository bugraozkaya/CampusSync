package com.bugra.campussync.utils

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("campus_sync_prefs", Context.MODE_PRIVATE)

    fun saveAuthData(token: String, role: String, username: String) {
        prefs.edit().apply {
            putString("ACCESS_TOKEN", token)
            putString("USER_ROLE", role)
            putString("USERNAME", username)
            apply()
        }
    }

    fun saveProfileInfo(nameSurname: String, department: String, position: String) {
        prefs.edit().apply {
            putString("NAME_SURNAME", nameSurname)
            putString("DEPARTMENT", department)
            putString("POSITION", position)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString("ACCESS_TOKEN", null)
    fun getRole(): String? = prefs.getString("USER_ROLE", null)
    fun getUsername(): String? = prefs.getString("USERNAME", null)
    
    fun getNameSurname(): String? = prefs.getString("NAME_SURNAME", null)
    fun getDepartment(): String? = prefs.getString("DEPARTMENT", null)
    fun getPosition(): String? = prefs.getString("POSITION", null)

    fun isProfileComplete(): Boolean {
        return !getNameSurname().isNullOrBlank() && 
               !getDepartment().isNullOrBlank() && 
               !getPosition().isNullOrBlank()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
