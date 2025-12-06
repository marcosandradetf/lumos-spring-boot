package com.lumos.repository

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.auth0.android.jwt.JWT
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
import com.lumos.notifications.NotificationManager
import retrofit2.Retrofit


// Repositório que faz a autenticação e lida com os tokens
class AuthRepository(
    retrofit: Retrofit,
    private val secureStorage: SecureStorage,
    private val app: Application,
    private val notificationManager: NotificationManager
) {

    fun isAuthenticated(): Boolean {
        return !secureStorage.getAccessToken().isNullOrBlank()
    }

    private val authApi: AuthApi = retrofit.create(AuthApi::class.java)

//    suspend fun login(
//        username: String,
//        email: String,
//        password: String,
//    ): RequestResult<Unit> {
//        val loginRequest = LoginRequest(username, email, password)
//
//        return try {
//            // Faz a chamada de forma assíncrona e aguarda a resposta
//            val response = ApiExecutor.execute { authApi.login(request = loginRequest) }
//
//            when (response) {
//                is Success -> {
//                    val body = response.data
//
//                    secureStorage.saveTokens(body.accessToken, body.refreshToken)
//                    secureStorage.saveUserUuid(body.userUUID)
//                    val roles = body.roles.trim().split(' ').toSet()
//                    val teams = body.teams.trim().split(' ').toSet()
//                    if (roles.isNotEmpty() && teams.isNotEmpty())
//                        secureStorage.saveRoles(roles)
//                        secureStorage.saveTeams(teams)
//                    Success(Unit)
//                }
//
//                is SuccessEmptyBody -> {
//                    // Se seu login retorna 204 mas está ok, então trate isso como sucesso.
//                    ServerError(204, "Resposta 204 inesperada no login")
//                }
//
//                is NoInternet -> NoInternet
//                is ServerError -> ServerError(response.code, response.message)
//                is Timeout -> Timeout
//                is UnknownError -> UnknownError(response.error)
//            }
//
//        } catch (e: Exception) {
//            // Se der erro na requisição, trata a falha
//            Log.e("Login Error", "Erro ao fazer login: ${e.localizedMessage}")
//            RequestResult.ServerError(-1, "${e.localizedMessage}")
//        }
//    }

    suspend fun login(
        username: String,
        password: String,
    ): RequestResult<Unit> {

        val loginValue = username.trim()
        val finalValue =
            if (loginValue.filter { it.isDigit() }.length == 11)
                loginValue.filter { it.isDigit() }
            else
                loginValue
        println(finalValue)

        val loginRequest = LoginRequest(finalValue, finalValue, password)

        return try {
            val response = ApiExecutor.execute { authApi.login(request = loginRequest) }
            println(response)
            when (response) {
                is Success -> {
                    val body = response.data

                    val accessToken = body.accessToken
                    val refreshToken = body.refreshToken

                    // ------------------------------
                    // 1. JWT Decode
                    // ------------------------------
                    val jwt = JWT(accessToken)

                    val userId = jwt.getClaim("sub").asString()
                    val roles = jwt.getClaim("scope").asString()?.split(" ") ?: emptyList()
                    val fullName = jwt.getClaim("fullname").asString()
                    val emailDecoded = jwt.getClaim("email").asString()

                    // ------------------------------
                    // 2. Save on SecureStorage
                    // ------------------------------
                    secureStorage.saveTokens(accessToken, refreshToken)

                    userId?.let { secureStorage.saveUserUuid(it)
                        notificationManager.subscribeInTopics(setOf(it))
                    }
                    secureStorage.saveRoles(roles.toSet())
                    secureStorage.setFullName(fullName ?: "")
                    secureStorage.saveEmail(emailDecoded ?: "")

                    Success(Unit)
                }

                is SuccessEmptyBody -> ServerError(204, "Resposta 204 inesperada no login")
                is NoInternet -> NoInternet
                is ServerError -> ServerError(response.code, response.message)
                is Timeout -> Timeout
                is UnknownError -> UnknownError(response.error)
            }
        } catch (e: Exception) {
            Log.e("Login Error", "Erro ao fazer login: ${e.localizedMessage}")
            ServerError(-1, "${e.localizedMessage}")
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