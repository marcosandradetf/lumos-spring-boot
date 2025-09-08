package com.lumos.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object NavEvents {
    private val _route = MutableStateFlow<String?>(null)
    val route: StateFlow<String?> = _route

    fun publish(route: String?) {
        _route.value = route
    }
}
