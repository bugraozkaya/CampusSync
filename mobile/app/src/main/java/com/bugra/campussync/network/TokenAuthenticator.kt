package com.bugra.campussync.network

import com.bugra.campussync.utils.TokenManager
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Route

class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val baseUrl: String
) : Authenticator {

    override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
        // Sonsuz döngüyü önle: zaten retry yapılmışsa çık
        if (response.request.header("X-Retry-Auth") != null) {
            tokenManager.clearAll()
            RetrofitClient.authToken = null
            SessionManager.triggerLogout()
            return null
        }

        val refreshToken = tokenManager.getRefreshToken() ?: run {
            SessionManager.triggerLogout()
            return null
        }

        val newAccessToken = refreshSync(refreshToken) ?: run {
            tokenManager.clearAll()
            RetrofitClient.authToken = null
            SessionManager.triggerLogout()
            return null
        }

        tokenManager.saveAuthData(
            token = newAccessToken,
            refreshToken = refreshToken,
            role = tokenManager.getRole() ?: "",
            username = tokenManager.getUsername() ?: "",
            mustChangePassword = tokenManager.getMustChangePassword()
        )
        RetrofitClient.authToken = newAccessToken

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .header("X-Retry-Auth", "true")
            .build()
    }

    private fun refreshSync(refreshToken: String): String? {
        return try {
            val client = OkHttpClient()
            val body = """{"refresh":"$refreshToken"}"""
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${baseUrl}api/v1/token/refresh/")
                .post(body)
                .build()

            val resp = client.newCall(request).execute()
            if (!resp.isSuccessful) return null

            val bodyStr = resp.body?.string() ?: return null
            Regex(""""access"\s*:\s*"([^"]+)"""").find(bodyStr)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }
}
