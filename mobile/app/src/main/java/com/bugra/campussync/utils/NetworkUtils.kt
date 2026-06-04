package com.bugra.campussync.utils

import com.bugra.campussync.network.NetworkResult
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeApiCall(apiCall: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(apiCall())
    } catch (e: HttpException) {
        val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
        val serverMsg = body?.let {
            Regex(""""(detail|error)"\s*:\s*"([^"]+)"""").find(it)?.groupValues?.get(2)
        }
        val msg = serverMsg ?: when (e.code()) {
            401 -> "Yetkisiz erişim. Lütfen tekrar giriş yapın."
            403 -> "Bu işlem için yetkiniz yok."
            404 -> "İstenen kaynak bulunamadı."
            423 -> "Hesabınız geçici olarak kilitlendi."
            500 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
            else -> "Bir hata oluştu (${e.code()})."
        }
        NetworkResult.Error(msg, e.code())
    } catch (e: IOException) {
        NetworkResult.Error("Sunucuya ulaşılamıyor. İnternet bağlantınızı kontrol edin.")
    } catch (e: Exception) {
        NetworkResult.Error("Beklenmedik bir hata oluştu: ${e.localizedMessage}")
    }
}
