package com.bugra.campussync.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "campus_sync_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAuthData(token: String, refreshToken: String = "", role: String, username: String, mustChangePassword: Boolean = false) {
        prefs.edit()
            .putString("ACCESS_TOKEN", token)
            .putString("REFRESH_TOKEN", refreshToken)
            .putString("USER_ROLE", role)
            .putString("USERNAME", username)
            .putBoolean("MUST_CHANGE_PASSWORD", mustChangePassword)
            .apply()
    }

    fun saveProfileInfo(nameSurname: String, department: String, position: String) {
        prefs.edit()
            .putString("NAME_SURNAME", nameSurname)
            .putString("DEPARTMENT", department)
            .putString("POSITION", position)
            .apply()
    }

    fun saveUserInfo(firstName: String?, lastName: String?, title: String? = null) {
        prefs.edit()
            .putString("FIRST_NAME", firstName ?: "")
            .putString("LAST_NAME", lastName ?: "")
            .putString("TITLE", title ?: "")
            .apply()
    }

    fun getToken(): String? = prefs.getString("ACCESS_TOKEN", null)
    fun getRefreshToken(): String? = prefs.getString("REFRESH_TOKEN", null)
    fun getRole(): String? = prefs.getString("USER_ROLE", null)
    fun getUsername(): String? = prefs.getString("USERNAME", null)
    fun getMustChangePassword(): Boolean = prefs.getBoolean("MUST_CHANGE_PASSWORD", false)

    fun clearMustChangePassword() {
        prefs.edit().putBoolean("MUST_CHANGE_PASSWORD", false).apply()
    }

    fun getFirstName(): String? = prefs.getString("FIRST_NAME", null)?.ifBlank { null }
    fun getLastName(): String? = prefs.getString("LAST_NAME", null)?.ifBlank { null }
    fun getTitle(): String? = prefs.getString("TITLE", null)?.ifBlank { null }

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
