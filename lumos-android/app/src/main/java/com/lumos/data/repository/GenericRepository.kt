package com.lumos.data.repository

import com.lumos.data.api.ApiService
import com.lumos.data.api.UpdateEntity
import com.lumos.data.api.UtilApi

class GenericRepository(
    api: ApiService,
) {
    private val utilApi = api.createApi(UtilApi::class.java)

    suspend fun setEntity(
        request: UpdateEntity,
    ): Boolean {
        return try {
            val response = utilApi.updateEntity(request)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }


}
