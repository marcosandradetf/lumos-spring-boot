package com.lumos.api

import com.lumos.domain.model.RemoteConfigResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface RemoteConfigApi {

    @GET("api/remote-config/get-config")
    suspend fun getRemoteConfig(
        @Header("X-App-Id") appId: String,
        @Header("X-Platform") platform: String,
        @Header("X-App-Build") appBuild: Long
    ): RemoteConfigResponse

    @POST("api/remote-config/post-payload")
    suspend fun sendPayload(
        @Body payLoad: List<Any>,
        @Query("type") type: String
    )
}
