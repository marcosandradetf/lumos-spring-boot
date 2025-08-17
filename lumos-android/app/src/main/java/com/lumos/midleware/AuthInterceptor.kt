package com.lumos.midleware

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.lumos.MainActivity
import com.lumos.api.AuthApi
import com.lumos.domain.model.LoginResponse
import com.lumos.notifications.NotificationManager
import com.lumos.utils.ConnectivityUtils.BASE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthInterceptor(
    context: Context,
    private val secureStorage: SecureStorage,
) : Interceptor {

    private val notificationManager = NotificationManager(context, secureStorage)
    private val appContext = context.applicationContext

    companion object {
        private val refreshMutex = Mutex() // 🔒 garante exclusividade
    }

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
            response.close()

            val newTokens = runBlocking(Dispatchers.IO) {
                refreshMutex.withLock {
                    val currentToken = secureStorage.getAccessToken()
                    if (currentToken != null && currentToken != accessToken) {
                        // Outra thread já renovou enquanto esperávamos
                        return@withLock LoginResponse(
                            currentToken,
                            0L,
                            "",
                            "",
                            "",
                            ""
                        )
                    }

                    val refreshToken = secureStorage.getRefreshToken()

                    val retrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val authApi = retrofit.create(AuthApi::class.java)

                    try {
                        val tokenResponse = authApi.refreshToken(refreshToken = refreshToken)
                        if (tokenResponse.isSuccessful) {
                            val tokens = tokenResponse.body()
                            if (tokens != null) {
                                secureStorage.saveTokens(tokens.accessToken, tokens.refreshToken) // Salvando novos tokens
                            }
                            tokenResponse.body()
                        } else {
                            authApi.logout(refreshToken = refreshToken)
                            notificationManager.unsubscribeFromSavedTopics()
                            secureStorage.clearAll()

                            val intent = Intent(appContext, MainActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            appContext.startActivity(intent)

                            null
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "Exception",
                            "Erro durante a renovação do token: ${e.localizedMessage}"
                        )
                        null
                    }
                }
            }

            if (newTokens != null) {
                val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${newTokens.accessToken}")
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

