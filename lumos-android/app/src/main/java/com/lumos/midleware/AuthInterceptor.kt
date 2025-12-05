package com.lumos.midleware

import android.util.Log
import com.lumos.utils.ConnectivityUtils.BASE_URL
import com.lumos.utils.SessionManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

class AuthInterceptor(
    private val secureStorage: SecureStorage,
    private val client: OkHttpClient
) : Interceptor {

    companion object {
        private val refreshMutex = Mutex()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url.encodedPath
        val accessToken = secureStorage.getAccessToken()

        // Não intercepta rotas de auth
        val isAuthEndpoint = url.startsWith("/spring/api/mobile/auth")
        if (isAuthEndpoint) {
            return chain.proceed(chain.request())
        }

        // Aplica token
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = chain.proceed(request)

        if (response.code == 401) {
            response.close()

            val newToken = runBlocking {
                refreshMutex.withLock {

                    // Caso outro thread já tenha renovado
                    val updatedToken = secureStorage.getAccessToken()
                    if (updatedToken != null && updatedToken != accessToken) {
                        return@withLock updatedToken
                    }

                    val refresh = secureStorage.getRefreshToken()
                    val body = """{"refreshToken":"$refresh"}"""
                        .toRequestBody("application/json".toMediaType())

                    val refreshRequest = Request.Builder()
                        .url(BASE_URL + "api/mobile/auth/v2/refresh-token")
                        .post(body)
                        .build()

                    return@withLock try {
                        client.newCall(refreshRequest).execute().use { tokenResponse ->
                            if (!tokenResponse.isSuccessful) {
                                SessionManager.setSessionExpired(true)
                                null
                            } else {
                                val json = tokenResponse.body.string()
                                val obj = JSONObject(json)
                                val newAccess = obj.optString("accessToken", "")
                                val newRefresh = obj.optString("refreshToken", "")

                                if (newAccess.isNotEmpty() && newRefresh.isNotEmpty()) {
                                    secureStorage.saveTokens(newAccess, newRefresh)
                                    newAccess
                                } else {
                                    null
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AuthInterceptor", "Erro ao renovar token: ${e.localizedMessage}")
                        null
                    }
                }
            }

            if (newToken != null) {
                val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $newToken")
                    .build()

                return chain.proceed(newRequest)
            }
        }

        return response
    }
}
