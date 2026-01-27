package com.lumos.repository

import com.lumos.api.RemoteConfigApi
import com.lumos.domain.model.RemoteConfigResponse
import com.lumos.domain.model.SyncQueueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit

class RemoteConfigRepository(
    retrofit: Retrofit,
    authenticatedRetrofit: Retrofit
) {

    private val api = retrofit.create(RemoteConfigApi::class.java)
    private val apiAuthenticated = authenticatedRetrofit.create(RemoteConfigApi::class.java)

    suspend fun fetchConfig(
        appId: String,
        platform: String,
        build: Long
    ): RemoteConfigResponse = withContext(Dispatchers.IO) {
        api.getRemoteConfig(appId, platform, build)
    }

    suspend fun sendPayload(payload: List<Any>, type: String) = withContext(Dispatchers.IO) {
        apiAuthenticated.sendPayload(payload, type)
    }

}
