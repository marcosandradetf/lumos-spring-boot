package com.lumos.utils

import kotlinx.coroutines.flow.MutableStateFlow

object SessionManager {
    private val _loggedIn = MutableStateFlow(false)
    val loggedIn = _loggedIn
    private val _loggedOut = MutableStateFlow(false)
    val loggedOut = _loggedOut
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired = _sessionExpired

    private val _checkingSession = MutableStateFlow(true)
    val checkingSession = _checkingSession
    private val _activationRequired = MutableStateFlow(false)
    val activationRequired = _activationRequired
    private val _pendingActivationCpf = MutableStateFlow<String?>(null)
    val pendingActivationCpf = _pendingActivationCpf

    fun setLoggedIn(value: Boolean) {
        loggedIn.value = value
    }

    fun setLoggedOut(value: Boolean) {
        _loggedOut.value = value
    }

    fun setSessionExpired(value: Boolean) {
        _sessionExpired.value = value
    }

    fun setCheckingSession(value: Boolean) {
        _checkingSession.value = value
    }

    fun setActivationRequired(value: Boolean) {
        _activationRequired.value = value
    }

    fun setPendingActivationCpf(value: String?) {
        _pendingActivationCpf.value = value
    }

}
