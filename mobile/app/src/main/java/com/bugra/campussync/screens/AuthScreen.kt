package com.bugra.campussync.screens // Kendi paket adını kontrol et

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugra.campussync.network.LoginRequest
import com.bugra.campussync.network.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
    var isLoginMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Yükleniyor durumu ve hata mesajı için değişkenler
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Arka plan işlemleri (Network isteği) için Coroutine Scope
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLoginMode) "Hoş Geldiniz" else "Yeni Hesap Oluştur",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Kullanıcı Adı") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Şifre") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hata mesajı varsa ekranda kırmızı şekilde göster
        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = Color.Red, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (isLoginMode) {
                    // GİRİŞ YAPMA İŞLEMİ BAŞLIYOR
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = ""
                        try {
                            // Django'ya gönderilecek veriyi hazırlıyoruz
                            val request = LoginRequest(username, password)
                            // İsteği atıp cevabı bekliyoruz
                            val response = RetrofitClient.apiService.login(request)

                            // Başarılı olursa Token loglara yazdırılır ve ana sayfaya geçilir
                            Log.d("AUTH_SUCCESS", "Başarılı! Access Token: ${response.access}")
                            onLoginSuccess()

                        } catch (e: Exception) {
                            // Hata olursa (Örn: Yanlış şifre veya sunucu kapalı)
                            Log.e("AUTH_ERROR", "Giriş Hatası: ${e.message}")
                            errorMessage = "Giriş başarısız. Bilgilerinizi veya internetinizi kontrol edin."
                        } finally {
                            isLoading = false
                        }
                    }
                } else {
                    // İleride buraya Kayıt Olma (Register) API isteğini yazacağız
                    errorMessage = "Kayıt olma özelliği henüz eklenmedi."
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            // Yüklenme (istek atma) sırasında butonu tıklanamaz yap
            enabled = !isLoading
        ) {
            // Yükleniyorsa butonun içinde dönen ikon göster, yoksa yazıyı göster
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(text = if (isLoginMode) "Giriş Yap" else "Kayıt Ol", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = {
            isLoginMode = !isLoginMode
            errorMessage = "" // Mod değişince hatayı temizle
        }) {
            Text(text = if (isLoginMode) "Hesabın yok mu? Kayıt Ol" else "Zaten hesabın var mı? Giriş Yap")
        }
    }
}