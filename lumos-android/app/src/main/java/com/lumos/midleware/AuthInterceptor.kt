package com.lumos.midleware

import android.content.Context
import android.util.Log
import androidx.compose.runtime.rememberCoroutineScope
import com.lumos.data.api.AuthApi
import com.lumos.domain.model.RefreshTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.http.Header

import java.util.concurrent.atomic.AtomicBoolean

class AuthInterceptor(
    private val secureStorage: SecureStorage
) : Interceptor {

    private val isRefreshing = AtomicBoolean(false)

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = secureStorage.getAccessToken()

        // Se não for uma requisição de API, apenas passa a requisição como está
        val request = if (!chain.request().url.toString().contains("/api/mobile/auth/")) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        } else {
            return chain.proceed(chain.request())
        }

        Log.e("intercept", secureStorage.getRefreshToken().toString())

        var response = chain.proceed(request)

        // Se o token expirar (status 401), tenta renovar o token
        if (response.code == 401) {
            CoroutineScope(Dispatchers.IO).launch {


                val refreshToken = secureStorage.getRefreshToken()

                // Usando Retrofit para renovar o token
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.seuservidor.com/") // URL base da sua API
                    .build()

                val authApi = retrofit.create(AuthApi::class.java)

                val newAccessToken = try {
                    // Realiza a chamada de refresh token de forma assíncrona
                    val response = authApi.refreshToken(refreshToken = refreshToken)

                    if (response.isSuccessful) {
                        // Retorna o novo token de acesso se a resposta for bem-sucedida
                        response.body()?.accessToken
                    } else {
                        // Trata a falha na requisição (por exemplo, token de refresh inválido)
                        Log.e("Refresh Token Error", "Erro ao renovar o token: ${response.code()}")
                        null
                    }
                } catch (e: Exception) {
                    // Captura exceções, como problemas de rede
                    Log.e("Exception", "Erro durante a renovação do token: ${e.localizedMessage}")
                    null
                }

                // Se obtivemos um novo access token, tenta a requisição novamente com o novo token
                if (newAccessToken != null) {
                    secureStorage.saveAccessToken(newAccessToken)

                    // Retenta a requisição com o novo token
                    val newRequest = request.newBuilder()
                        .addHeader("Authorization", "Bearer $newAccessToken")
                        .build()

                    // Aqui, fazemos a chamada novamente com o novo token
                    response = chain.proceed(newRequest)
                }

            }
        }

        // Caso o código da resposta seja 403, você pode adicionar um log ou tratá-lo de outra forma
        if (response.code == 403) {
            Log.e("Response", "403 - Forbidden")
        }

        return response
    }
}

