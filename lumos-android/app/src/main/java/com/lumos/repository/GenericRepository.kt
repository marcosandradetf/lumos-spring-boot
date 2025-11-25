package com.lumos.repository

import android.util.Log
import com.lumos.api.ApiExecutor
import com.lumos.api.RequestResult
import com.lumos.api.UpdateEntity
import com.lumos.api.UtilApi
import retrofit2.Retrofit

class GenericRepository(
    api: Retrofit,
) {
    private val utilApi = api.create(UtilApi::class.java)

    suspend fun setEntity(
        request: UpdateEntity,
    ): RequestResult<Unit> {
        val response = ApiExecutor.execute { utilApi.updateEntity(request) }
        return when (response) {
            is RequestResult.Success -> {
                RequestResult.Success(Unit)
            }

            is RequestResult.SuccessEmptyBody -> RequestResult.SuccessEmptyBody
            is RequestResult.NoInternet -> RequestResult.NoInternet
            is RequestResult.Timeout -> RequestResult.Timeout
            is RequestResult.ServerError -> RequestResult.ServerError(
                response.code,
                response.message
            )

            is RequestResult.UnknownError -> {
                Log.e("Sync", "Erro desconhecido", response.error)
                RequestResult.UnknownError(response.error)
            }
        }
    }


}
