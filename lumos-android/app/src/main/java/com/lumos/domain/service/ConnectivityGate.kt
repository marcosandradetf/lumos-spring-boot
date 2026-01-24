package com.lumos.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class ConnectivityGate(
    private val client: OkHttpClient
) {
    private val pingUrl = "https://api.thryon.com.br/spring/ping"

    suspend fun canReachServer(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(pingUrl)
                    .head()
                    .build()

                client.newCall(request).execute().use {
                    it.isSuccessful
                }
            } catch (_: Exception) {
                false
            }
        }
}
