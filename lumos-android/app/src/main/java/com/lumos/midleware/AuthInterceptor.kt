package com.lumos.midleware

import android.content.Context
import android.util.Log
import com.lumos.data.api.AuthApi
import com.lumos.domain.model.RefreshTokenRequest
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.http.Header

class AuthInterceptor
    (
    private val authApi: AuthApi,
    private val secureStorage: SecureStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = secureStorage.getAccessToken()
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        Log.e("intercept", secureStorage.getRefreshToken().toString())

        val response = chain.proceed(request)

        // Se o token expirar, tente renová-lo
        if (response.code == 401) {

            synchronized(this) {
                Log.e("Refresh Token", "Refresh Token")
                val refreshToken = secureStorage.getRefreshToken() ?: return response
                val newAccessToken = refreshAccessToken(refreshToken)

                if (!newAccessToken.isNullOrEmpty()) {
                    secureStorage.saveTokens(newAccessToken, refreshToken)

                    // Refaz a requisição com o novo token
                    val newRequest = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $newAccessToken")
                        .build()
                    return chain.proceed(newRequest)
                }
            }
        } else if (response.code == 403) {
            Log.e("Response", "403")
        }


        return response
    }

    private fun refreshAccessToken(refreshToken: String): String? {
        return try {
            val response = authApi.refreshToken("application/json",refreshToken).execute()
            if (response.isSuccessful) response.body()?.accessToken else null
        } catch (e: Exception) {
            null
        }
    }
}
