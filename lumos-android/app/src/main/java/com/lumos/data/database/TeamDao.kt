package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.domain.model.OperationalUsers
import com.lumos.domain.model.Team
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsers(users: List<OperationalUsers>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTeams(teams: List<Team>)

    @Query("SELECT * FROM OperationalUsers")
    fun getUsersFlow(): Flow<List<OperationalUsers>>

    @Query("SELECT * FROM Team")
    fun getTeamsFlow(): Flow<List<Team>>

}
