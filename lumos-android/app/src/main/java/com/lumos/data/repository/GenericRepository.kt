package com.lumos.data.repository

import android.util.Log
import com.lumos.data.api.ApiExecutor
import com.lumos.data.api.ApiService
import com.lumos.data.api.RequestResult
import com.lumos.data.api.UpdateEntity
import com.lumos.data.api.UtilApi
import com.lumos.worker.SyncManager

class GenericRepository(
    api: ApiService,
) {
    private val utilApi = api.createApi(UtilApi::class.java)

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
