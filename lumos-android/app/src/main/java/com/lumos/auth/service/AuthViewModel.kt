package com.lumos.auth.service

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.lumos.midleware.SecureStorage
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthViewModel : ViewModel() {
    private val _isAuthenticated = mutableStateOf(false)
    val isAuthenticated: State<Boolean> get() = _isAuthenticated

    val authService: AuthService = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthService::class.java)

    val authRepository = AuthRepository(authService, SecureStorage)

    fun checkAuthentication(context: Context) {
        _isAuthenticated.value = !SecureStorage.getAccessToken(context).isNullOrBlank()
    }

    fun login(
        context: Context,
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        authRepository.login(context, username, password, onSuccess, onFailure)
    }

    fun logout() {
        _isAuthenticated.value = false
    }

    fun authenticate(context: Context) {
        _isAuthenticated.value = !SecureStorage.getAccessToken(context).isNullOrBlank()
    }

}
