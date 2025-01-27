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


// Repositório que faz a autenticação e lida com os tokens
class AuthRepository (
    private val authApi: AuthApi,
    private val secureStorage: SecureStorage,
    private val context: Context
) {

    fun isAuthenticated(): Boolean {
        return !secureStorage.getAccessToken().isNullOrBlank()
    }

    fun login(
        username: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val loginRequest = LoginRequest(username, email, password)


        authApi.login("application/json", loginRequest).enqueue(object :
            Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        secureStorage.saveTokens(body.accessToken, body.refreshToken)
                        onSuccess()
                    } else {
//                        showToast("Resposta do servidor está vazia")
                        onFailure()
                    }
                } else {
                    // Obter o corpo do erro como uma string
                    val errorMessage = response.errorBody()?.string()

                    // Converter o JSON para JsonObject e acessar o campo "message"
                    val jsonObject = JsonParser.parseString(errorMessage).asJsonObject
                    val message = jsonObject.get("message")?.asString ?: "Erro desconhecido"

                    // Exibir a mensagem de erro
//                    showToast(message)

                    onFailure()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
//                showToast(context, "Erro: ${t.localizedMessage}")
                Log.e("LoginError", "Falha na requisição: ${t.localizedMessage}", t)
                onFailure()
            }
        })
    }

    fun logout(onComplete: () -> Unit) {
        var refreshToken = secureStorage.getRefreshToken()
        refreshToken = refreshToken?.replace("\"", "")?.trim()
        if (refreshToken != null) {
            authApi.logout("application/json", refreshToken).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    secureStorage.clearTokens()
                    onComplete()
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    secureStorage.clearTokens()
                    onComplete()
                }
            })
        } else {
            secureStorage.clearTokens()
            onComplete()
        }
    }

    // Função simplificada para mostrar o Toast
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}