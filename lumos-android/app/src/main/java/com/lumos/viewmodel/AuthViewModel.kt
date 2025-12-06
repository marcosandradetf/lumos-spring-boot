package com.lumos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.api.RequestResult
import com.lumos.repository.AuthRepository
import com.lumos.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle

) : ViewModel() {
    var loading by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    fun login(
        username: String,
        password: String
    ) {
        viewModelScope.launch {
            val response = withContext(Dispatchers.IO) {
                authRepository.login(
                    username,
                    password,
                )
            }
            when (response) {
                is RequestResult.Success -> {
                    SessionManager.setLoggedIn(true)
                    message = "Login realizado com sucesso!"
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
            loading = false
            SessionManager.setLoggedOut(true)
        }
    }

}
