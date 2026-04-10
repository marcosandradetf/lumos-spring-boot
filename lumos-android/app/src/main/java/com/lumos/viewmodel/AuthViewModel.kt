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
    var activationCompleted by mutableStateOf(false)
    fun login(
        username: String,
        password: String
    ) {
        viewModelScope.launch {
            loading = true
            val response = withContext(Dispatchers.IO) {
                authRepository.login(
                    username,
                    password,
                )
            }
            loading = false
            when (response) {
                is RequestResult.Success -> {
                    SessionManager.setLoggedIn(true)
                    SessionManager.setActivationRequired(false)
                    message = "Login realizado com sucesso!"
                }

                is RequestResult.SuccessEmptyBody -> {
                    message = "Erro ao fazer login - EmptyBody. Tente Novamente ou informe o Admin!"
                }
                is RequestResult.NoInternet -> {
                    message = "Problema ao enviar requisição - Sem internet. Tente novamente!"
                }
                is RequestResult.ServerError -> {
                    if (response.errorCode == "USER_NOT_ACTIVATED") {
                        SessionManager.setPendingActivationCpf(username.filter { it.isDigit() }.takeIf { it.length == 11 })
                        SessionManager.setActivationRequired(true)
                        message = null
                    } else {
                        message = response.message ?: "Usuário/CPF ou Senha incorretos."
                    }
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

    fun activateFirstAccess(
        cpf: String,
        activationCode: String,
        newPassword: String,
        confirmPassword: String
    ) {
        if (cpf.filter { it.isDigit() }.length != 11) {
            message = "Informe um CPF válido."
            return
        }

        if (activationCode.isBlank()) {
            message = "Informe o código de ativação."
            return
        }

        if (newPassword.length < 8) {
            message = "A senha deve ter pelo menos 8 caracteres."
            return
        }

        if (newPassword != confirmPassword) {
            message = "As senhas não conferem."
            return
        }

        viewModelScope.launch {
            loading = true
            activationCompleted = false
            val response = withContext(Dispatchers.IO) {
                authRepository.activateFirstAccess(cpf, activationCode, newPassword)
            }
            loading = false

            when (response) {
                is RequestResult.Success,
                is RequestResult.SuccessEmptyBody -> {
                    SessionManager.setActivationRequired(false)
                    SessionManager.setPendingActivationCpf(null)
                    activationCompleted = true
                    message = "Conta ativada com sucesso. Faça login com sua nova senha."
                }

                is RequestResult.NoInternet -> {
                    message = "Problema ao enviar requisição - Sem internet. Tente novamente!"
                }

                is RequestResult.ServerError -> {
                    message = response.message ?: "Não foi possível concluir a ativação."
                }

                is RequestResult.Timeout -> {
                    message = "Problema ao enviar requisição - Internet Lenta. Tente novamente!"
                }

                is RequestResult.UnknownError -> {
                    message = "Erro ao ativar conta. Tente novamente ou informe o Admin!"
                }
            }
        }
    }

    fun consumeActivationCompleted() {
        activationCompleted = false
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
