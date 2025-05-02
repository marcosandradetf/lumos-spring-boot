package com.lumos.data.api

import com.lumos.domain.model.Contract
import com.lumos.domain.model.ExecutionDTO
import retrofit2.Response
import retrofit2.http.GET
import java.util.UUID

interface ExecutionApi {
    @GET("/api/mobile/execution/get-executions")
    suspend fun getExecutions(userUUID: UUID): Response<List<ExecutionDTO>>

}