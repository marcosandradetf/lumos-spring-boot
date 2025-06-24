package com.lumos.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.api.RequestResult
import com.lumos.midleware.SecureStorage
import com.lumos.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {
    private val _isAuthenticated = MutableStateFlow<Boolean?>(null)
    val isAuthenticated: StateFlow<Boolean?> = _isAuthenticated

    fun login(
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        viewModelScope.launch {
            val response = withContext(Dispatchers.IO) {
                authRepository.login(
                    username,
                    username,
                    password,
                )
            }

            when (response) {
                is RequestResult.Success -> {
                    onSuccess()
                }
                is RequestResult.SuccessEmptyBody -> onFailure()
                is RequestResult.NoInternet -> onFailure()
                is RequestResult.ServerError -> onFailure()
                is RequestResult.Timeout -> onFailure()
                is RequestResult.UnknownError -> onFailure()
            }
        }
    }

     fun logout(
        onSuccess: () -> Unit,
    ) {
         viewModelScope.launch(Dispatchers.Main) {
             _isAuthenticated.value = false
             authRepository.logout(onSuccess)
         }
    }

    fun authenticate() {
        viewModelScope.launch {
            val accessToken = secureStorage.getAccessToken()
            _isAuthenticated.value = accessToken != null
        }
    }

}
