package com.lumos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.api.RequestResult
import com.lumos.repository.AuthRepository
import com.lumos.midleware.SecureStorage
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
    var loading by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    fun login(
        username: String,
        password: String,
        onSuccess: () -> Unit,
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
                    message = "Login realizado com sucesso!"
                    onSuccess()
                }

                is RequestResult.SuccessEmptyBody -> {
                    message = "Erro ao fazer login - EmptyBody. Tente Novamente ou informe o Admin!"
                }
                is RequestResult.NoInternet -> {
                    message = "Problema ao enviar requisição - Sem internet. Tente novamente!"
                }
                is RequestResult.ServerError -> {
                    message = "Usuário/CPF ou Senha incorretos."
                }
                is RequestResult.Timeout -> {
                    message = "Problema ao enviar requisição - Internet Lenta. Tente novamente!"
                }
                is RequestResult.UnknownError -> {
                    message = "Erro ao fazer login - Desconhecido. Tente Novamente ou informe o Admin!"
                }
            }
        }
    }

    fun logout(
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            loading = true
            withContext(Dispatchers.IO) {
                authRepository.logout(onSuccess)
            }
            _isAuthenticated.value = false
            loading = false
        }
    }

    fun authenticate() {
        viewModelScope.launch {
            loading = true
            try {
                val accessToken = withContext(Dispatchers.IO) {
                    secureStorage.getAccessToken()
                }
                _isAuthenticated.value = accessToken != null
            } catch (_: Exception) {

            } finally {
                loading = false
            }
        }
    }

}
