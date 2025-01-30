package com.lumos.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.midleware.SecureStorage
import com.lumos.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel (
    private val authRepository: AuthRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {
    private val _isAuthenticated = mutableStateOf(false)
    val isAuthenticated: State<Boolean> get() = _isAuthenticated


    fun checkAuthentication() {
        _isAuthenticated.value = authRepository.isAuthenticated()
    }

    suspend fun login(
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        authRepository.login(username, username, password, onSuccess, onFailure)
    }

    suspend fun logout(
        onSuccess: () -> Unit,
    ) {
        _isAuthenticated.value = false
        authRepository.logout(onSuccess)
    }

    fun authenticate(context: Context) {
        viewModelScope.launch {
            val accessToken = secureStorage.getAccessToken()
            _isAuthenticated.value = accessToken != null
        }
    }

}
