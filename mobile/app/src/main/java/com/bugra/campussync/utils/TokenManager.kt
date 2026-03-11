package com.bugra.campussync.utils // Kendi paket adına dikkat et

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    // "campus_sync_prefs" adında gizli bir dosya oluşturuyoruz
    private val prefs: SharedPreferences = context.getSharedPreferences("campus_sync_prefs", Context.MODE_PRIVATE)

    // Token'ı kaydetme fonksiyonu
    fun saveToken(token: String) {
        prefs.edit().putString("ACCESS_TOKEN", token).apply()
    }

    // Token'ı okuma fonksiyonu
    fun getToken(): String? {
        return prefs.getString("ACCESS_TOKEN", null)
    }

    // Çıkış yapıldığında Token'ı silme fonksiyonu
    fun clearToken() {
        prefs.edit().remove("ACCESS_TOKEN").apply()
    }
}