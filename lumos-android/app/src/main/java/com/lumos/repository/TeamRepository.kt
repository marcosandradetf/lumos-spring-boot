package com.lumos.repository

import android.app.Application
import android.util.Log
import com.lumos.api.ApiExecutor
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.api.RequestResult.SuccessEmptyBody
import com.lumos.api.TeamApi
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.OperationalUser
import com.lumos.domain.model.SendTeamEdit
import com.lumos.domain.model.Team
import com.lumos.midleware.SecureStorage
import com.lumos.worker.SyncManager
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit

class TeamRepository(
    private val db: AppDatabase,
    api: Retrofit,
    private val secureStorage: SecureStorage,
    private val app: Application
) {
    private val teamApi = api.create(TeamApi::class.java)

    suspend fun queueUpdateTeam() {
        SyncManager.queueUpdateTeam(
            context = app.applicationContext,
            db = db,
        )
    }

    suspend fun queueGetStock() {
        SyncManager.queueGetStock(
            context = app.applicationContext,
            db = db
        )
    }

    fun getUsersFlow(): Flow<List<OperationalUser>> {
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

    suspend fun callPostUpdateTeam(): RequestResult<Unit> {
        val team = SendTeamEdit(
            idTeam = secureStorage.getTeamId(),
            userIds = secureStorage.getOperationalUsers().toList()
        )

        val response = ApiExecutor.execute {
            teamApi.updateTeam(team)
        }

        Log.e("callPostUpdateTeam", response.toString())

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
                RequestResult.UnknownError(response.error)
            }
        }

    }

    suspend fun setTeamAndQueue(teamId: Long, operationalUsers: Set<String>) {
        try {
            val currentTeamId = secureStorage.getTeamId()
            secureStorage.saveOperationalUsers(operationalUsers)
            secureStorage.setLastTeamCheck()
            queueUpdateTeam()

            if(currentTeamId != teamId) {
                secureStorage.setTeamId(teamId)
                db.stockDao().deleteStock()
                queueGetStock()
            }
        } catch (e: Exception) {
            throw e
        }
    }

}