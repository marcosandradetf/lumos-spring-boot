package com.lumos.midleware

import android.content.Context
import com.lumos.service.AuthService
import com.lumos.service.RefreshTokenRequest
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context, private val authService: AuthService) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = SecureStorage.getAccessToken(context)
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = chain.proceed(request)

        // Se o token expirar, tente renová-lo
        if (response.code == 401) {
            synchronized(this) {
                val refreshToken = SecureStorage.getRefreshToken(context) ?: return response
                val newAccessToken = refreshAccessToken(refreshToken)

                if (!newAccessToken.isNullOrEmpty()) {
                    SecureStorage.saveTokens(context, newAccessToken, refreshToken)

                    // Refaz a requisição com o novo token
                    val newRequest = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $newAccessToken")
                        .build()
                    return chain.proceed(newRequest)
                }
            }
        }

        return response
    }

    private fun refreshAccessToken(refreshToken: String): String? {
        return try {
            val response = authService.refreshToken(RefreshTokenRequest(refreshToken)).execute()
            if (response.isSuccessful) response.body()?.accessToken else null
        } catch (e: Exception) {
            null
        }
    }
}
