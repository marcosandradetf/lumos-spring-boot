package com.lumos.midleware

import android.content.Context
import android.content.Intent
import android.util.Log
import com.lumos.MainActivity
import com.lumos.data.api.AuthApi
import com.lumos.notifications.NotificationManager
import com.lumos.utils.ConnectivityUtils.BASE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicBoolean

class AuthInterceptor(
    context: Context,
    private val secureStorage: SecureStorage,
) : Interceptor {

    private val isRefreshing = AtomicBoolean(false)
    private val notificationManager = NotificationManager(context, secureStorage)
    private val appContext = context.applicationContext

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

        val response = chain.proceed(request)

        // Se o token expirar (status 401), tenta renovar o token
        if (response.code == 401) {
            // Usando runBlocking para aguardar a renovação do token de forma síncrona
            val newAccessToken = runBlocking(Dispatchers.IO) {
                val refreshToken = secureStorage.getRefreshToken()

                // Usando Retrofit para renovar o token
                val retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL) // URL base da sua API
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val authApi = retrofit.create(AuthApi::class.java)

                try {
                    // Realiza a chamada de refresh token de forma assíncrona, mas aguarda de forma síncrona
                    val tokenResponse = authApi.refreshToken(refreshToken = refreshToken)

                    if (tokenResponse.isSuccessful) {
                        // Retorna o novo token de acesso se a resposta for bem-sucedida
                        tokenResponse.body()?.accessToken
                    } else {
                        // Trata a falha na requisição (por exemplo, token de refresh inválido)
                        Log.e(
                            "Refresh Token Error",
                            "Erro ao renovar o token: ${tokenResponse.code()}"
                        )
                        authApi.logout(refreshToken = refreshToken)
                        notificationManager.unsubscribeFromSavedTopics()
                        secureStorage.clearAll()

                        val intent = Intent(appContext, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        appContext.startActivity(intent)

                        null
                    }
                } catch (e: Exception) {
                    // Captura exceções, como problemas de rede
                    Log.e("Exception", "Erro durante a renovação do token: ${e.localizedMessage}")
                    null
                }
            }

            // Se obtivemos um novo access token, tenta a requisição novamente com o novo token
            if (newAccessToken != null) {
                secureStorage.saveAccessToken(newAccessToken)

                // Aqui, fazemos a chamada novamente com o novo token
                val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $newAccessToken")
                    .build()
                return chain.proceed(newRequest)
            }
        }

        // Caso o código da resposta seja 403, você pode adicionar um log ou tratá-lo de outra forma
        if (response.code == 403) {
            Log.e("Response", "403 - Forbidden")
        }

        return response
    }

}

