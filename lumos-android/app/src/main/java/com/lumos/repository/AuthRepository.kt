package com.lumos.repository

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.lumos.api.ApiExecutor
import com.lumos.api.AuthApi
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.NoInternet
import com.lumos.api.RequestResult.ServerError
import com.lumos.api.RequestResult.Success
import com.lumos.api.RequestResult.SuccessEmptyBody
import com.lumos.api.RequestResult.Timeout
import com.lumos.api.RequestResult.UnknownError
import com.lumos.domain.model.LoginRequest
import com.lumos.midleware.SecureStorage
import retrofit2.Retrofit


// Repositório que faz a autenticação e lida com os tokens
class AuthRepository(
    retrofit: Retrofit,
    private val secureStorage: SecureStorage,
    private val app: Application
) {

    fun isAuthenticated(): Boolean {
        return !secureStorage.getAccessToken().isNullOrBlank()
    }

    private val authApi: AuthApi = retrofit.create(AuthApi::class.java)

    suspend fun login(
        username: String,
        email: String,
        password: String,
    ): RequestResult<Unit> {
        val loginRequest = LoginRequest(username, email, password)

        return try {
            // Faz a chamada de forma assíncrona e aguarda a resposta
            val response = ApiExecutor.execute { authApi.login(request = loginRequest) }

            when (response) {
                is Success -> {
                    val body = response.data

                    secureStorage.saveTokens(body.accessToken, body.refreshToken, body.userUUID)
                    val roles = body.roles.trim().split(' ').toSet()
                    val teams = body.teams.trim().split(' ').toSet()
                    if (roles.isNotEmpty() && teams.isNotEmpty())
                        secureStorage.saveAssignments(
                            roles,
                            teams
                        )
                    Success(Unit)
                }

                is SuccessEmptyBody -> {
                    // Se seu login retorna 204 mas está ok, então trate isso como sucesso.
                    ServerError(204, "Resposta 204 inesperada no login")
                }

                is NoInternet -> NoInternet
                is ServerError -> ServerError(response.code, response.message)
                is Timeout -> Timeout
                is UnknownError -> UnknownError(response.error)
            }

        } catch (e: Exception) {
            // Se der erro na requisição, trata a falha
            Log.e("Login Error", "Erro ao fazer login: ${e.localizedMessage}")
            RequestResult.ServerError(-1, "${e.localizedMessage}")
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
        Toast.makeText(app.applicationContext, message, Toast.LENGTH_SHORT).show()
    }

}