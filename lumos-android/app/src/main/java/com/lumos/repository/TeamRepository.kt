package com.lumos.repository

import android.app.Application
import android.util.Log
import androidx.room.withTransaction
import com.lumos.api.ApiExecutor
import com.lumos.api.ApiService
import com.lumos.api.StockApi
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.api.RequestResult.SuccessEmptyBody
import com.lumos.api.TeamApi
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.MaterialStock
import com.lumos.domain.model.OperationalUsers
import com.lumos.domain.model.OrderMaterial
import com.lumos.domain.model.OrderMaterialItem
import com.lumos.domain.model.SendTeamEdit
import com.lumos.domain.model.Stockist
import com.lumos.domain.model.Team
import com.lumos.midleware.SecureStorage
import com.lumos.utils.Utils.uuidToShortCodeWithPrefix
import com.lumos.worker.SyncManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TeamRepository(
    private val db: AppDatabase,
    api: ApiService,
    private val secureStorage: SecureStorage,
    private val app: Application
) {
    private val teamApi = api.createApi(TeamApi::class.java)

    suspend fun queueUpdateTeam() {
        SyncManager.queueUpdateTeam(
            context = app.applicationContext,
            db = db,
        )
    }

    fun getUsersFlow(): Flow<List<OperationalUsers>> {
        return db.teamDao().getUsersFlow()
    }

    fun getTeamsFlow(): Flow<List<Team>> {
        return db.teamDao().getTeamsFlow()
    }


    suspend fun callGetOperationalAndTeams(): RequestResult<Unit> {
        val response = ApiExecutor.execute {
            teamApi.getOperationalAndTeams()
        }

        return when (response) {
            is RequestResult.Success -> {
                db.teamDao().insertUsers(response.data.users)
                db.teamDao().insertTeams(response.data.teams)
                RequestResult.Success(Unit)
            }

            is SuccessEmptyBody -> {
                ServerError(204, "Resposta 204 inesperada")
            }

            is RequestResult.NoInternet -> {
                RequestResult.NoInternet
            }

            is RequestResult.Timeout -> RequestResult.Timeout
            is ServerError -> ServerError(response.code, response.message)
            is RequestResult.UnknownError -> {
                Log.e("Sync", "Erro desconhecido", response.error)
                RequestResult.UnknownError(response.error)
            }
        }
    }

    suspend fun callPostUpdateTeam(orderId: String): RequestResult<Unit> {
        val team = SendTeamEdit(
            idTeam = secureStorage.getTeamId(),
            userIds = secureStorage.getOperationalUsers().toList()
        )

        val response = ApiExecutor.execute {
            teamApi.updateTeam(team)
        }

        return when (response) {
            is RequestResult.Success -> {
                RequestResult.Success(Unit)
            }

            is SuccessEmptyBody -> {
                RequestResult.Success(Unit)
            }

            is RequestResult.NoInternet -> {
                RequestResult.NoInternet
            }

            is RequestResult.Timeout -> RequestResult.Timeout
            is ServerError -> ServerError(response.code, response.message)
            is RequestResult.UnknownError -> {
                Log.e("Sync", "Erro desconhecido", response.error)
                RequestResult.UnknownError(response.error)
            }
        }

    }


}