package com.lumos.api

import com.lumos.domain.model.DirectExecutionDTOResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class RenewUrlResponse(
    val newUrl: String,
    val expiryAt: Long
)
interface MinioApi {

    @POST("api/mobile/minio/v1/update-object-public-url")
    suspend fun updateObjectPublicUrl(
        @Path("objectUri") objectUri: String
    ): Response<RenewUrlResponse>

}