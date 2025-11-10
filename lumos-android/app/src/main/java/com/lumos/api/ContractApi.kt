package com.lumos.api

import com.lumos.domain.model.Contract
import com.lumos.domain.model.ContractItemBalance
import com.lumos.domain.model.Item
import retrofit2.Response
import retrofit2.http.GET

interface ContractApi {
    @GET("api/mobile/contracts/get-contracts")
    suspend fun getContracts(): Response<List<Contract>>

    @GET("api/mobile/contracts/get-reference-items")
    suspend fun getItems(): Response<List<Item>>

    @GET("api/mobile/v1/contracts/get-contracts-balance")
    suspend fun getContractItemBalance(): Response<List<ContractItemBalance>>

}