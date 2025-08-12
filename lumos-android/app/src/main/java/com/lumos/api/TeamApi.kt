package com.lumos.api

import com.lumos.domain.model.OperationalAndTeamsResponse
import com.lumos.domain.model.SendTeamEdit
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TeamApi {
    @GET("api/mobile/user/get-operational-users")
    suspend fun getOperationalAndTeams(): Response<OperationalAndTeamsResponse>

    @POST("api/mobile/teams/update-team")
    suspend fun updateTeam(
        @Body team: SendTeamEdit
    ): Response<Void>

}