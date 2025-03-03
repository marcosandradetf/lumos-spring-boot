package com.lumos.data.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.gson.JsonParser
import com.lumos.midleware.SecureStorage
import com.lumos.data.api.AuthApi
import com.lumos.domain.model.LoginRequest
import com.lumos.domain.model.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit


// Repositório que faz a autenticação e lida com os tokens
class AuthRepository(
    retrofit: Retrofit,
    private val secureStorage: SecureStorage,
    private val context: Context
) {

    fun isAuthenticated(): Boolean {
        return !secureStorage.getAccessToken().isNullOrBlank()
    }

    private val authApi: AuthApi = retrofit.create(AuthApi::class.java)

    suspend fun login(
        username: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val loginRequest = LoginRequest(username, email, password)

        try {
            // Faz a chamada de forma assíncrona e aguarda a resposta
            val response = authApi.login(request = loginRequest)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    secureStorage.saveTokens(body.accessToken, body.refreshToken, body.userUUID)
                    onSuccess()
                }
            } else {
                val body = response.body()
                if (body != null) {
                    val errorMessage = response.errorBody()?.string() ?: "Erro desconhecido"
                    showToast(errorMessage)
                    onFailure()
                }
            }
        } catch (e: Exception) {
            // Se der erro na requisição, trata a falha
            Log.e("Login Error", "Erro ao fazer login: ${e.localizedMessage}")
            onFailure()
        }
    }


    suspend fun logout(onComplete: () -> Unit) {
        val refreshToken = secureStorage.getRefreshToken()

        try {
            // Faz a chamada de forma assíncrona e aguarda a resposta
            val response = authApi.logout(refreshToken = refreshToken)

            if (response.isSuccessful) {
                onComplete()
            }
        } catch (e: Exception) {
            // Se der erro na requisição, trata a falha
            Log.e("Logout Error", "Erro ao fazer Logout: ${e.localizedMessage}")
        }
    }

    // Função simplificada para mostrar o Toast
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}