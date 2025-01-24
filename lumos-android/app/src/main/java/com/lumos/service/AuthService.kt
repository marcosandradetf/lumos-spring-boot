package com.lumos.service

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.lumos.midleware.SecureStorage

import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Header

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(
    val accessToken: String,
    val expiresIn: Long,
    val roles: String,
    val refreshToken: String
)

data class RefreshTokenRequest(val refreshToken: String)
data class RefreshTokenResponse(val accessToken: String, val expiresIn: Long, val roles: String)


interface AuthService {
    @POST("/api/mobile/auth/login")
    fun login(
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: LoginRequest
    ): Call<LoginResponse>

    @POST("/refresh-token")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<RefreshTokenResponse>

    @POST("/logout")
    fun logout(@Body refreshToken: String): Call<Void>


}

// Repositório que faz a autenticação e lida com os tokens
class AuthRepository(
    private val authService: AuthService,
    private val secureStorage: SecureStorage
) {

    fun login(
        context: Context,
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val loginRequest = LoginRequest(username, password)

        authService.login("application/json",loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.e("t", "body")
                        secureStorage.saveTokens(context, body.accessToken, body.refreshToken)
                        onSuccess()
                    } else {
                        Log.e("t", "bodyNull")
//                        showToast(context, "Resposta do servidor está vazia")
                        onFailure()
                    }
                } else {
//                    showToast(context, "Erro: ${response.code()}")
                    Log.e("LoginError", "Falha na requisição: $response")
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

    fun logout(context: Context, onComplete: () -> Unit) {
        val refreshToken = secureStorage.getRefreshToken(context)

        if (refreshToken != null) {
            authService.logout(refreshToken).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    secureStorage.clearTokens(context)
                    onComplete()
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    secureStorage.clearTokens(context)
                    onComplete()
                }
            })
        } else {
            secureStorage.clearTokens(context)
            onComplete()
        }
    }

    // Função simplificada para mostrar o Toast
    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
